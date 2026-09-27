# Promotion Images (Scheduled Banner API)

This feature uses the existing global image upload flow (`/public/images`) and adds a promotion mapping layer so UI can fetch only relevant banners by key/tag and placement with an active time window.

## Example (Diwali campaign)

1. Admin uploads banner images globally (existing API), getting `mediaId`s.
2. Admin creates promotion mappings:
   - `promoKey = diwali`
   - `placement = homepage_hero` (or `experience_page_top`)
   - `startAt = 2026-10-28T00:00:00`
   - `endAt = 2026-11-05T23:59:59`
   - `priority = 1` (lower number is higher priority)
3. UI calls:
   - List: `GET /public/promotions/images?key=diwali&placement=homepage_hero`
   - Single: `GET /public/promotions/images?key=diwali&placement=homepage_hero&single=true`

Behavior:
- If current time is inside the window, API returns matching image(s), sorted by `priority ASC`.
- If no active records exist (campaign not started, already ended, or manually inactive), API returns empty list for list API or `null` payload for single API.
- Returned URLs are sourced from media variants (`heroUrl`, `thumbnailUrl`, `originalUrl`) and fall back to the standard public image URL when a variant is unavailable.

## New APIs

### Admin APIs
- `POST /admin/promotions/assets` — create promotion mapping
- `PUT /admin/promotions/assets/{id}` — update mapping
- `DELETE /admin/promotions/assets/{id}` — delete mapping
- `GET /admin/promotions/assets?key=&placement=&isActive=` — list/filter mappings

### Public API
- `GET /public/promotions/images?key={tag}&placement={optional}&single={true|false}`

## UI integration notes

- UI controls layout and rendering; backend only filters by key, placement, activation window, and active flag.
- For rotating banners, call list API and rotate client-side.
- For a single hero banner, call single API and render the returned item.

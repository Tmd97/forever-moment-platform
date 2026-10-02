# Platform Service CI Guide

This document explains the Continuous Integration (CI) setup for the platform service in simple terms: **WHAT** it does, **WHY** it exists, **HOW** it works, and **HOW TO VERIFY** it.

---

## 1) WHAT this CI pipeline does

The platform repository now has **two workflow files**:

- `.github/workflows/ci.yml` (trigger file)
- `.github/workflows/reusable-ci.yml` (shared CI engine)

How this design works:

- `ci.yml` listens to `push` and `pull_request` on `main`.
- `ci.yml` calls `reusable-ci.yml`.
- `reusable-ci.yml` performs the actual build/test/docker/publish steps.

Main CI stages:

1. **Checkout** source code
2. **Setup Java 17** with Maven dependency cache
3. **Build and test** Maven modules (`clean verify`) from repo root
4. **Optional security scan placeholder** (ready for tooling)
5. **Docker build** to verify containerization
6. **DockerHub login + image push** on push events:
   - always pushes immutable tag: `<dockerhub_repo>:${{ github.sha }}`
   - pushes `latest` only when branch is `main`

---

## 2) WHY this exists

CI gives fast feedback so broken changes are caught early:

- Confirms the project compiles and tests pass across modules.
- Confirms Docker image can be built from the current code.
- Publishes versioned container images for downstream CD systems.
- Keeps delivery safer by using immutable commit-SHA image tags.

---

## 3) HOW it works (step by step)

### Trigger and cancellation behavior

- `ci.yml` triggers on pushes/PRs to `main`.
- Uses workflow `concurrency` with `cancel-in-progress: true` by ref.
  - If you push multiple times to the same branch quickly, old in-flight runs are canceled.

### Reusable workflow behavior

- `reusable-ci.yml` is the central workflow logic for Java services.
- Platform uses it locally.
- Booking/payment/gateway can call this same reusable file from their own `ci.yml`.
- This removes duplicate YAML across service repos.

### Java + Maven build/test

- Uses `actions/setup-java` with:
  - `distribution: temurin`
  - `java-version: 17`
  - `cache: maven`
- Maven command selection:
  - Uses `./mvnw` if present.
  - Falls back to `mvn` when wrapper script is absent.
- Runs:
  - `clean verify`
  - This is multi-module-safe because it runs at root `pom.xml`.

### Security/dependency scan placeholder

There is an intentional placeholder step where you can add tools like:

- OWASP Dependency Check
- Trivy
- Snyk

This keeps the pipeline extensible without blocking current delivery.

### Docker image build and publish

- Always builds local validation image: `platform-ci-local:${GITHUB_SHA}`
- On `push` events:
  - Logs in to DockerHub using:
    - `DOCKERHUB_USERNAME` (secret)
    - `DOCKERHUB_TOKEN` (secret)
  - Tags/pushes immutable image:
    - `<dockerhub-repository>:${GITHUB_SHA}`
  - If branch is `main`, also tags/pushes:
    - `<dockerhub-repository>:latest`

---

## 4) HOW TO VERIFY

## A. Verify workflow wiring

1. Open **Actions** tab in GitHub.
2. Confirm workflow **Platform CI** appears.
3. Create a PR to `main` and verify pipeline runs.

Expected on PR:

- Build/test runs.
- Docker image build runs.
- Docker push steps are skipped (push-only).

Expected on push to main:

- All build/test + docker steps run.
- SHA image tag is pushed.
- `latest` tag is pushed.

## B. Verify required repository configuration

Set these before relying on image push:

- **Secrets**
  - `DOCKERHUB_USERNAME`
  - `DOCKERHUB_TOKEN`

Also confirm `dockerhub_repository` value in the calling `ci.yml` is correct, for example:

- `groote123/moment-forever-platform`

## C. Verify CI/CD separation

This workflow is **CI-only**:

- It builds, tests, scans (placeholder), and publishes images.
- It does **not** run deployment commands.
- It does **not** include `kubectl`, ArgoCD, Helm, or environment rollout logic.

---

## 5) CI vs CD (quick beginner explanation)

- **CI (Continuous Integration)** = validate code changes (compile, test, package).
- **CD (Continuous Delivery/Deployment)** = roll validated artifacts into environments.

In this repository:

- CI is implemented by `.github/workflows/ci.yml` + `.github/workflows/reusable-ci.yml`.
- CD should live in separate deployment workflows/pipelines (not in this file).

---

## 6) Placeholders / values your team must provide

1. DockerHub credentials as repository secrets:
   - `DOCKERHUB_USERNAME`
   - `DOCKERHUB_TOKEN`
2. (Optional) pick and integrate a security scan tool in the placeholder step.
3. In other service repositories, verify the reusable workflow reference points to the correct platform repo path (currently `groote123/forever-moment-platform-master/.github/workflows/reusable-ci.yml@main`).

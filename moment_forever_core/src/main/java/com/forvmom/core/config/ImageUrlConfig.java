package com.forvmom.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code app.image.*} properties and builds the URLs under which stored
 * images are served.
 *
 * <p>
 * Image bytes live in MongoDB GridFS and are addressed by storage file name, so
 * clients never receive a raw store reference — only a URL produced here. Routing
 * every URL through this one class means switching between serving images from
 * the service and serving them from a CDN is a configuration change
 * ({@code app.image.use-cdn}) rather than a code change.
 */
@Component
@ConfigurationProperties(prefix = "app.image")
public class ImageUrlConfig {

    private String baseUrl = "/api/platform";
    private String publicBaseUrl = "/public/images";
    private String adminBaseUrl = "/admin/images";
    private String cdnBaseUrl;
    private int cacheMaxAge = 31536000; // 1 year in seconds
    private boolean useCdn = false;

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getAdminBaseUrl() {
        return adminBaseUrl;
    }

    public void setAdminBaseUrl(String adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
    }

    public String getCdnBaseUrl() {
        return cdnBaseUrl;
    }

    public void setCdnBaseUrl(String cdnBaseUrl) {
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public int getCacheMaxAge() {
        return cacheMaxAge;
    }

    public void setCacheMaxAge(int cacheMaxAge) {
        this.cacheMaxAge = cacheMaxAge;
    }

    public boolean isUseCdn() {
        return useCdn;
    }

    public void setUseCdn(boolean useCdn) {
        this.useCdn = useCdn;
    }

    /**
     * Builds the public URL for an image, preferring the CDN when one is enabled
     * and configured.
     *
     * @param storageFileName GridFS storage file name of the image
     * @return the public URL clients should use to fetch the image
     */
    public String buildPublicUrl(String storageFileName) {
        if (useCdn && cdnBaseUrl != null) {
            return joinUrl(cdnBaseUrl, "fetch", storageFileName);
        }
        return joinUrl(baseUrl, publicBaseUrl, "fetch", storageFileName);
    }

    /**
     * Builds the admin URL for an image, addressed by media id rather than storage
     * file name so admins can act on the metadata record.
     *
     * @param mediaId identifier of the media record
     * @return the admin-facing image URL
     */
    public String buildAdminUrl(Long mediaId) {
        return joinUrl(baseUrl, adminBaseUrl, String.valueOf(mediaId));
    }

    /**
     * Builds the thumbnail URL for an image.
     *
     * <p>
     * Currently identical to {@link #buildPublicUrl(String)} because no resizing
     * pipeline exists yet; it is kept separate so callers already point at the
     * right seam once one is added.
     *
     * @param storageFileName GridFS storage file name of the image
     * @return the thumbnail URL
     */
    public String buildThumbnailUrl(String storageFileName) {
        if (useCdn && cdnBaseUrl != null) {
            return joinUrl(cdnBaseUrl, "fetch", storageFileName);
        }
        return joinUrl(baseUrl, publicBaseUrl, "fetch", storageFileName);
    }

    private String joinUrl(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            String token = part.trim();
            if (result.length() == 0) {
                result.append(stripTrailingSlash(token));
                continue;
            }
            if (result.charAt(result.length() - 1) != '/') {
                result.append('/');
            }
            result.append(stripLeadingSlash(token));
        }
        return result.toString();
    }

    private String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 1 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private String stripLeadingSlash(String value) {
        int start = 0;
        while (start < value.length() && value.charAt(start) == '/') {
            start++;
        }
        return value.substring(start);
    }
}
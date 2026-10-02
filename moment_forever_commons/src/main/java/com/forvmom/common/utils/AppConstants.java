package com.forvmom.common.utils;

public class AppConstants {

    // Application
    public static final String APP_NAME = "Moment Forever Platform";
    public static final String VERSION = "1.0.0";

    // File Upload
    public static final String UPLOAD_DIR = "uploads/";
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    // Validation
    public static final int MIN_CATEGORY_NAME_LENGTH = 2;
    public static final int MAX_CATEGORY_NAME_LENGTH = 100;
    public static final int MIN_EXPERIENCE_TITLE_LENGTH = 5;
    public static final int MAX_EXPERIENCE_TITLE_LENGTH = 200;

    // Date/Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // File Paths
    public static final String EXPERIENCE_UPLOAD_PATH = "experiences/";
    public static final String CATEGORY_UPLOAD_PATH = "categories/";
    public static final String SERVICE_TYPE_UPLOAD_PATH = "service-types/";

    // Booking
    public static final String BOOKING_REFERENCE_PREFIX = "MFB-";
    public static final int MIN_GUEST_COUNT = 1;
    public static final int MAX_GUEST_COUNT = 50;

    // Support
    public static final String SUPPORT_REFERENCE_PREFIX = "SUP-";

    // Cache
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_SERVICE_TYPES = "serviceTypes";
    public static final long CACHE_TTL = 3600; // 1 hour in seconds

    // API
    public static final String API_PREFIX = "/com/example/moment_forever/common";
    public static final String ADMIN_API_PREFIX = "/com/example/moment_forever/common/admin";
    public static final String PUBLIC_API_PREFIX = "/com/example/moment_forever/common/public";

    // Messages
    public static final String MSG_SUCCESS = "Success";
    public static final String MSG_CREATED = "Created successfully";
    public static final String MSG_UPDATED = "Updated successfully";
    public static final String MSG_DELETED = "Deleted successfully";
    public static final String MSG_NOT_FOUND = "Resource not found";
    public static final String MSG_VALIDATION_ERROR = "Validation failed";
    public static final String MSG_INTERNAL_ERROR = "Internal server error";
    public static final String MSG_FETCHED = "fetched Successfully";

    //security constants
    public static final String SECRET_KEY = "your-secret-key-min-256-bit-change-in-production";
    public static final long EXPIRATION_TIME = 864_000_000; // 10 days
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // Public URLs that don't require authentication
    public static final String[] PUBLIC_URLS = {
            "/api/auth/**",           // Authentication endpoints
            "/error",                 // Error handling
            "/favicon.ico"           // Favicon
    };
}
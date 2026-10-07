package com.farmconnect.android.util;

import com.farmconnect.android.network.ApiClient;

/**
 * Centralizes product-image URL resolution for the whole app.
 *
 * Handles:
 *  - Cloudinary/external HTTPS image URLs -> preserved as-is
 *  - Old absolute backend image URLs -> rebuilt against current backend
 *  - Relative /uploads paths -> rebuilt against current backend
 *  - Relative product image paths -> rebuilt against current backend
 */
public final class ImageUrlHelper {

    private ImageUrlHelper() {
    }

    public static String resolve(String rawImageUrl) {
        if (rawImageUrl == null) {
            return null;
        }

        String trimmed = rawImageUrl.trim();

        if (trimmed.isEmpty()) {
            return trimmed;
        }

        /*
         * Cloudinary and other external HTTPS image URLs must be preserved.
         *
         * Example:
         * https://res.cloudinary.com/.../product-2.jpg
         *
         * Do NOT convert this into the FarmConnect backend URL.
         */
        if (trimmed.startsWith("https://res.cloudinary.com/")) {
            return trimmed;
        }

        String currentBase = ApiClient.BASE_URL.endsWith("/")
                ? ApiClient.BASE_URL.substring(0, ApiClient.BASE_URL.length() - 1)
                : ApiClient.BASE_URL;

        /*
         * Old absolute backend URL:
         *
         * http://old-ip:8080/uploads/product.jpg
         *
         * Strip the old host and rebuild using the current backend.
         */
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            int schemeEnd = trimmed.indexOf("://") + 3;
            int pathStart = trimmed.indexOf('/', schemeEnd);

            if (pathStart < 0) {
                return currentBase;
            }

            String path = trimmed.substring(pathStart);
            return currentBase + normalizePath(path);
        }

        /*
         * Relative path:
         *
         * /uploads/product.jpg
         * uploads/product.jpg
         * products/product.jpg
         */
        return currentBase + normalizePath(trimmed);
    }

    private static String normalizePath(String path) {
        String p = path.startsWith("/") ? path : "/" + path;

        return p.startsWith("/uploads/")
                ? p
                : "/uploads" + p;
    }
}
package org.link.linkvault.service;

public final class AuditDetailFormatter {

    private AuditDetailFormatter() {}

    private static final int MAX_DETAIL_LENGTH = 490;

    public static String format(String... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(keyValuePairs[i]).append('=').append(safe(keyValuePairs[i + 1]));
        }
        return truncate(sb.toString());
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIdx = email.indexOf('@');
        if (atIdx <= 2) return "**@" + email.substring(atIdx + 1);
        return email.substring(0, 2) + "***@" + email.substring(atIdx + 1);
    }

    public static String maskUrl(String url) {
        if (url == null) return null;
        // Extract host portion only
        String work = url;
        int schemeEnd = work.indexOf("://");
        if (schemeEnd >= 0) {
            work = work.substring(schemeEnd + 3);
        }
        int pathStart = work.indexOf('/');
        if (pathStart >= 0) {
            work = work.substring(0, pathStart);
        }
        return work + "/***";
    }

    public static String applyMasking(String details, String maskingLevel) {
        if (details == null || "NONE".equals(maskingLevel)) {
            return details;
        }
        String result = details;
        // BASIC and STRICT: mask emails
        result = maskEmailsInText(result);
        // STRICT: mask URLs
        if ("STRICT".equals(maskingLevel)) {
            result = maskUrlsInText(result);
        }
        return result;
    }

    private static String maskEmailsInText(String text) {
        // Simple pattern: look for word@word.word patterns in values
        StringBuilder sb = new StringBuilder();
        String[] parts = text.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            String part = parts[i];
            // Check if this is a key=value pair where value contains @
            int eqIdx = part.indexOf('=');
            if (eqIdx >= 0) {
                String val = part.substring(eqIdx + 1);
                if (val.contains("@") && val.contains(".")) {
                    sb.append(part, 0, eqIdx + 1).append(maskEmail(val));
                    continue;
                }
            }
            sb.append(part);
        }
        return sb.toString();
    }

    private static String maskUrlsInText(String text) {
        StringBuilder sb = new StringBuilder();
        String[] parts = text.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            String part = parts[i];
            int eqIdx = part.indexOf('=');
            if (eqIdx >= 0) {
                String val = part.substring(eqIdx + 1);
                if (val.startsWith("http://") || val.startsWith("https://")) {
                    sb.append(part, 0, eqIdx + 1).append(maskUrl(val));
                    continue;
                }
            }
            sb.append(part);
        }
        return sb.toString();
    }

    private static String safe(String value) {
        return value != null ? value : "null";
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_DETAIL_LENGTH) return s;
        return s.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}

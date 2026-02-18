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

    private static String safe(String value) {
        return value != null ? value : "null";
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_DETAIL_LENGTH) return s;
        return s.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}

package org.link.linkvault.service;

public final class AuditActionCodes {

    private AuditActionCodes() {}

    // User management
    public static final String USER_CREATE = "ADMIN_USER_CREATE";
    public static final String USER_UPDATE = "ADMIN_USER_UPDATE";
    public static final String USER_DELETE = "ADMIN_USER_DELETE";
    public static final String USER_TOGGLE = "ADMIN_USER_TOGGLE";
    public static final String USER_BULK_DEACTIVATE = "ADMIN_USER_BULK_DEACTIVATE";

    // Tag management
    public static final String TAG_MERGE = "ADMIN_TAG_MERGE";
    public static final String TAG_CLEANUP = "ADMIN_TAG_CLEANUP";
    public static final String TAG_DELETE = "ADMIN_TAG_DELETE";

    // Backup/Restore
    public static final String BACKUP_CREATE = "BACKUP_CREATE";
    public static final String BACKUP_RESTORE = "BACKUP_RESTORE";

    // Invitation management
    public static final String INVITATION_CREATE = "ADMIN_INVITATION_CREATE";
    public static final String INVITATION_TOGGLE = "ADMIN_INVITATION_TOGGLE";
    public static final String INVITATION_DELETE = "ADMIN_INVITATION_DELETE";

    // Menu management
    public static final String MENU_CREATE = "ADMIN_MENU_CREATE";
    public static final String MENU_UPDATE = "ADMIN_MENU_UPDATE";
    public static final String MENU_DELETE = "ADMIN_MENU_DELETE";
    public static final String MENU_TOGGLE = "ADMIN_MENU_TOGGLE";
    public static final String MENU_REORDER = "ADMIN_MENU_REORDER";

    // Permission management
    public static final String PERMISSION_TOGGLE = "ADMIN_PERMISSION_TOGGLE";

    // QnA management
    public static final String QNA_CREATE = "ADMIN_QNA_CREATE";
    public static final String QNA_UPDATE = "ADMIN_QNA_UPDATE";
    public static final String QNA_STATUS_CHANGE = "ADMIN_QNA_STATUS_CHANGE";
    public static final String QNA_DELETE = "ADMIN_QNA_DELETE";

    // Announcement management
    public static final String ANNOUNCEMENT_CREATE = "ADMIN_ANNOUNCEMENT_CREATE";
    public static final String ANNOUNCEMENT_UPDATE = "ADMIN_ANNOUNCEMENT_UPDATE";
    public static final String ANNOUNCEMENT_STATUS = "ADMIN_ANNOUNCEMENT_STATUS";
    public static final String ANNOUNCEMENT_DELETE = "ADMIN_ANNOUNCEMENT_DELETE";

    // Report moderation
    public static final String REPORT_REVIEW = "ADMIN_REPORT_REVIEW";
    public static final String AUTO_DISABLE_USER = "AUTO_DISABLE_USER";
    public static final String BULK_SOFT_DELETE_CONTENT = "BULK_SOFT_DELETE_CONTENT";

    // Privacy policy
    public static final String PRIVACY_POLICY_UPDATE = "ADMIN_PRIVACY_POLICY_UPDATE";

    // System settings
    public static final String SETTINGS_UPDATE = "ADMIN_SETTINGS_UPDATE";
    public static final String AUDIT_POLICY_UPDATE = "ADMIN_AUDIT_POLICY_UPDATE";

    // Comment moderation
    public static final String COMMENT_SOFT_DELETE = "SOFT_DELETE_COMMENT";
    public static final String COMMENT_RESTORE = "RESTORE_COMMENT";
    public static final String COMMENT_PURGE = "PURGE_COMMENT";

    // Bookmark operations
    public static final String BOOKMARK_CREATE = "CREATE_BOOKMARK";
    public static final String BOOKMARK_UPDATE = "UPDATE_BOOKMARK";
    public static final String BOOKMARK_SOFT_DELETE = "SOFT_DELETE_BOOKMARK";
    public static final String BOOKMARK_RESTORE = "RESTORE_BOOKMARK";
    public static final String BOOKMARK_PURGE = "PURGE_BOOKMARK";
}

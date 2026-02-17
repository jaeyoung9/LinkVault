package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.entity.*;
import org.link.linkvault.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FolderRepository folderRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MenuItemRepository menuItemRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final PrivacyPolicyRepository privacyPolicyRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.seed-sample-data:true}")
    private boolean seedSampleData;

    @Override
    @Transactional
    public void run(String... args) {
        initPermissions();
        initMenuItems();
        initPrivacyPolicy();
        initSystemSettings();

        if (seedSampleData) {
            initUsers();
            initInvitationCode();
        } else {
            initBootstrapAdmin();
        }
    }

    private void initBootstrapAdmin() {
        if (userRepository.count() > 0) {
            return;
        }
        String adminPassword = System.getenv("LINKVAULT_ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("No LINKVAULT_ADMIN_PASSWORD env var set; skipping admin bootstrap");
            return;
        }
        String adminEmail = System.getenv().getOrDefault("LINKVAULT_ADMIN_EMAIL", "admin@linkvault.com");
        User admin = User.builder()
                .username("admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Bootstrap admin created (password from env)");
    }

    private void initPermissions() {
        if (permissionRepository.count() > 0) {
            return;
        }

        log.info("Initializing permissions...");

        List<Permission> permissions = Arrays.asList(
                new Permission("USER_MANAGE", "Create, edit, and delete users", "ADMIN"),
                new Permission("MENU_MANAGE", "Create, edit, and delete menu items", "ADMIN"),
                new Permission("INVITE_ISSUE", "Create invitation codes", "ADMIN"),
                new Permission("INVITE_REVOKE", "Revoke invitation codes", "ADMIN"),
                new Permission("AUDIT_VIEW", "View audit log entries", "ADMIN"),
                new Permission("BACKUP_RUN", "Create and restore backups", "ADMIN"),
                new Permission("VIEW_STATS", "View system statistics", "ADMIN"),
                new Permission("MANAGE_ANNOUNCEMENTS", "Create, edit, and delete announcements", "ADMIN"),
                new Permission("MANAGE_TAGS", "Create, merge, and delete tags", "CONTENT"),
                new Permission("MANAGE_BOOKMARKS", "Edit bookmarks", "CONTENT"),
                new Permission("BOOKMARK_DELETE_ANY", "Soft-delete any bookmark", "CONTENT"),
                new Permission("BOOKMARK_RESTORE", "Restore soft-deleted bookmarks", "CONTENT"),
                new Permission("MANAGE_QNA", "Create, edit, and delete QnA articles", "CONTENT"),
                new Permission("COMMENT_HIDE", "Soft-delete any comment", "MODERATION"),
                new Permission("COMMENT_RESTORE", "Restore soft-deleted comments", "MODERATION"),
                new Permission("CONTENT_PURGE", "Permanently delete soft-deleted content", "MODERATION"),
                new Permission("COMMENT", "Post comments on bookmarks", "COMMUNITY"),
                new Permission("VOTE", "Vote on comments", "COMMUNITY"),
                new Permission("POST_CREATE", "Create posts/stories", "COMMUNITY"),
                new Permission("POST_EDIT_OWN", "Edit own posts", "COMMUNITY"),
                new Permission("POST_DELETE_OWN", "Delete own posts", "COMMUNITY"),
                new Permission("REPORT_SUBMIT", "Submit content reports", "COMMUNITY"),
                new Permission("REPORT_REVIEW", "Review content reports", "MODERATION"),
                new Permission("POST_MODERATE", "Moderate any post", "MODERATION"),
                new Permission("SYSTEM_SETTINGS", "Manage system settings", "ADMIN")
        );
        permissionRepository.saveAll(permissions);

        // Default permission assignments
        // COMMUNITY_ADMIN: all except CONTENT_PURGE (reserved for SUPER_ADMIN)
        assignPermissions(Role.COMMUNITY_ADMIN, Arrays.asList(
                "USER_MANAGE", "MENU_MANAGE", "INVITE_ISSUE", "INVITE_REVOKE",
                "AUDIT_VIEW", "BACKUP_RUN", "VIEW_STATS", "MANAGE_ANNOUNCEMENTS",
                "MANAGE_TAGS", "MANAGE_BOOKMARKS", "BOOKMARK_DELETE_ANY", "BOOKMARK_RESTORE",
                "MANAGE_QNA", "COMMENT_HIDE", "COMMENT_RESTORE",
                "COMMENT", "VOTE", "SYSTEM_SETTINGS"
        ));

        // MODERATOR: limited admin + community + moderation
        assignPermissions(Role.MODERATOR, Arrays.asList(
                "COMMENT_HIDE", "AUDIT_VIEW", "VIEW_STATS",
                "MANAGE_TAGS", "MANAGE_QNA", "COMMENT", "VOTE",
                "POST_CREATE", "POST_EDIT_OWN", "POST_DELETE_OWN", "REPORT_SUBMIT",
                "REPORT_REVIEW", "POST_MODERATE"
        ));

        // MEMBER: community only
        assignPermissions(Role.MEMBER, Arrays.asList(
                "COMMENT", "VOTE",
                "POST_CREATE", "POST_EDIT_OWN", "POST_DELETE_OWN", "REPORT_SUBMIT"
        ));

        log.info("Permissions initialized with default role assignments");
    }

    private void assignPermissions(Role role, List<String> permissionNames) {
        for (String name : permissionNames) {
            permissionRepository.findByName(name).ifPresent(permission ->
                    rolePermissionRepository.save(new RolePermission(role, permission)));
        }
    }

    private void initUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Initializing default users...");

        User admin = User.builder()
                .username("admin")
                .email("admin@linkvault.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build();
        admin = userRepository.save(admin);

        User user = User.builder()
                .username("user")
                .email("user@linkvault.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.MEMBER)
                .enabled(true)
                .build();
        userRepository.save(user);

        // Assign existing bookmarks/folders with no user to admin
        final User adminUser = admin;
        for (Bookmark bookmark : bookmarkRepository.findAll()) {
            if (bookmark.getUser() == null) {
                bookmark.setUser(adminUser);
            }
        }
        for (Folder folder : folderRepository.findAll()) {
            if (folder.getUser() == null) {
                folder.setUser(adminUser);
            }
        }

        log.info("Default users created: admin/admin123 (SUPER_ADMIN), user/user123 (MEMBER)");
    }

    private void initMenuItems() {
        if (menuItemRepository.count() > 0) {
            return;
        }

        log.info("Initializing menu items...");

        // User Sidebar menu items
        menuItemRepository.save(MenuItem.builder().label("Feed").url("/").menuType(MenuType.SIDEBAR)
                .displayOrder(1).visible(true).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Map Discover").url("/map").menuType(MenuType.SIDEBAR)
                .displayOrder(2).visible(true).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Saved").url("/saved").menuType(MenuType.SIDEBAR)
                .displayOrder(3).visible(true).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Admin Panel").url("/admin").menuType(MenuType.SIDEBAR)
                .displayOrder(4).visible(true).requiredRole(Role.MODERATOR).systemItem(true).createdBy("system").build());

        // Admin Sidebar menu items
        menuItemRepository.save(MenuItem.builder().label("Dashboard").url("/admin").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(1).visible(true).requiredPermission("VIEW_STATS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Users").url("/admin/users").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(2).visible(true).requiredPermission("USER_MANAGE").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("All Bookmarks").url("/admin/bookmarks").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(3).visible(true).requiredPermission("MANAGE_BOOKMARKS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Tag Management").url("/admin/tags").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(4).visible(true).requiredPermission("MANAGE_TAGS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Audit Log").url("/admin/audit").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(6).visible(true).requiredPermission("AUDIT_VIEW").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Menu Management").url("/admin/menus").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(7).visible(true).requiredPermission("MENU_MANAGE").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Permissions").url("/admin/permissions").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(8).visible(true).requiredRole(Role.SUPER_ADMIN).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Comments").url("/admin/comments").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(5).visible(true).requiredPermission("COMMENT_HIDE").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Invitations").url("/admin/invitations").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(5).visible(true).requiredPermission("INVITE_ISSUE").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Backup / Restore").url("/admin/backup").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(9).visible(true).requiredPermission("BACKUP_RUN").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Reports").url("/admin/reports").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(10).visible(true).requiredPermission("REPORT_REVIEW").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("QnA Management").url("/admin/qna").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(11).visible(true).requiredPermission("MANAGE_QNA").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Announcements").url("/admin/announcements").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(12).visible(true).requiredPermission("MANAGE_ANNOUNCEMENTS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Privacy Policy").url("/admin/privacy-policy").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(13).visible(true).requiredRole(Role.SUPER_ADMIN).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Settings").url("/admin/settings").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(14).visible(true).requiredPermission("SYSTEM_SETTINGS").systemItem(true).createdBy("system").build());

        log.info("Menu items initialized");
    }

    private void initInvitationCode() {
        if (invitationCodeRepository.count() > 0) {
            return;
        }

        log.info("Initializing default invitation code...");

        User admin = userRepository.findByUsername("admin").orElse(null);
        InvitationCode code = InvitationCode.builder()
                .code("WELCOME1")
                .createdBy(admin)
                .maxUses(1)
                .active(true)
                .note("Default test invitation code")
                .assignedRole(Role.MEMBER)
                .build();
        invitationCodeRepository.save(code);

        log.info("Default invitation code created: WELCOME1");
    }

    private void initPrivacyPolicy() {
        if (privacyPolicyRepository.count() > 0) {
            return;
        }

        log.info("Initializing default privacy policy...");

        User admin = userRepository.findByUsername("admin").orElse(null);
        String defaultContent = "<p style=\"font-weight: 600; margin-bottom: 8px;\">LinkVault Privacy Policy</p>\n" +
                "<p style=\"margin-bottom: 6px;\">Last updated: 2026-02-16</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">1. Information We Collect</p>\n" +
                "<p>When you register, we collect your username, email address, and password (stored as a one-way hash). As you use the service, we store bookmark URLs, titles, descriptions, tags, folder structures, comments, and vote activity that you create.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">2. How We Use Your Information</p>\n" +
                "<p>Your information is used solely to provide the LinkVault bookmark management service. Specifically: authenticating your identity, displaying your saved bookmarks and folders, enabling comments and community features, and generating aggregate system statistics visible to administrators.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">3. Data Storage</p>\n" +
                "<p>All data is stored on the server where this LinkVault instance is hosted. This service uses an in-memory database; data may be lost when the server restarts unless a backup has been created by an administrator. We recommend exporting your bookmarks regularly.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">4. Data Sharing</p>\n" +
                "<p>We do not sell, trade, or share your personal information with third parties. Your bookmarks and content are visible to other authenticated users and administrators of this LinkVault instance according to the permission system in place.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">5. Administrator Access</p>\n" +
                "<p>Administrators and moderators may view your bookmarks, comments, and account information for the purposes of content moderation, system maintenance, and backup/restore operations. Audit logs record administrative actions.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">6. Content Deletion</p>\n" +
                "<p>Deleted bookmarks and comments are soft-deleted (hidden from normal view) and may be restored by administrators. Permanent deletion (purging) is available to authorized administrators. You may request full account deletion by contacting an administrator.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">7. Cookies and Sessions</p>\n" +
                "<p>We use session cookies strictly for authentication. No third-party tracking cookies or analytics services are used.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">8. Changes to This Policy</p>\n" +
                "<p>This privacy policy may be updated at any time. Continued use of the service after changes constitutes acceptance of the revised policy.</p>\n" +
                "\n" +
                "<p style=\"font-weight: 600; margin-top: 12px;\">9. Contact</p>\n" +
                "<p>For privacy-related questions, contact the administrator of this LinkVault instance.</p>";

        PrivacyPolicy policy = PrivacyPolicy.builder()
                .content(defaultContent)
                .version(1)
                .active(true)
                .updatedBy(admin)
                .build();
        privacyPolicyRepository.save(policy);

        // Auto-consent admin user to v1
        if (admin != null) {
            admin.agreeToPrivacyPolicy(1);
            userRepository.save(admin);
        }

        log.info("Default privacy policy v1 created (admin auto-consented)");
    }

    private void initSystemSettings() {
        if (systemSettingsRepository.count() > 0) {
            return;
        }

        log.info("Initializing system settings...");

        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("file-vault.upload-path")
                .settingValue("./uploads/photos")
                .description("File upload directory path")
                .category("FILE_STORAGE")
                .build());

        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("file-vault.allowed-types")
                .settingValue("image/jpeg,image/png,image/gif,image/webp")
                .description("Comma-separated list of allowed MIME types")
                .category("FILE_STORAGE")
                .build());

        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("file-vault.max-file-size-mb")
                .settingValue("10")
                .description("Maximum file upload size in megabytes")
                .category("FILE_STORAGE")
                .build());

        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("report.auto-disable-threshold")
                .settingValue("5")
                .description("Number of actioned reports before user account is auto-disabled")
                .category("MODERATION")
                .build());

        log.info("System settings initialized with defaults");
    }
}

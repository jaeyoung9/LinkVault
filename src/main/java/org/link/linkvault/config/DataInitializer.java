package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.entity.*;
import org.link.linkvault.repository.*;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initPermissions();
        initUsers();
        initMenuItems();
        initInvitationCode();
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
                new Permission("VOTE", "Vote on comments", "COMMUNITY")
        );
        permissionRepository.saveAll(permissions);

        // Default permission assignments
        // COMMUNITY_ADMIN: all except CONTENT_PURGE (reserved for SUPER_ADMIN)
        assignPermissions(Role.COMMUNITY_ADMIN, Arrays.asList(
                "USER_MANAGE", "MENU_MANAGE", "INVITE_ISSUE", "INVITE_REVOKE",
                "AUDIT_VIEW", "BACKUP_RUN", "VIEW_STATS", "MANAGE_ANNOUNCEMENTS",
                "MANAGE_TAGS", "MANAGE_BOOKMARKS", "BOOKMARK_DELETE_ANY", "BOOKMARK_RESTORE",
                "MANAGE_QNA", "COMMENT_HIDE", "COMMENT_RESTORE",
                "COMMENT", "VOTE"
        ));

        // MODERATOR: limited admin + community
        assignPermissions(Role.MODERATOR, Arrays.asList(
                "COMMENT_HIDE", "AUDIT_VIEW", "VIEW_STATS",
                "MANAGE_TAGS", "MANAGE_QNA", "COMMENT", "VOTE"
        ));

        // MEMBER: community only
        assignPermissions(Role.MEMBER, Arrays.asList("COMMENT", "VOTE"));

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
        menuItemRepository.save(MenuItem.builder().label("All Bookmarks").url("/").menuType(MenuType.SIDEBAR)
                .displayOrder(1).visible(true).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Saved").url("/saved").menuType(MenuType.SIDEBAR)
                .displayOrder(2).visible(true).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Admin Panel").url("/admin").menuType(MenuType.SIDEBAR)
                .displayOrder(3).visible(true).requiredRole(Role.MODERATOR).systemItem(true).createdBy("system").build());

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
        menuItemRepository.save(MenuItem.builder().label("QnA Management").url("/admin/qna").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(10).visible(true).requiredPermission("MANAGE_QNA").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Announcements").url("/admin/announcements").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(11).visible(true).requiredPermission("MANAGE_ANNOUNCEMENTS").systemItem(true).createdBy("system").build());

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
}

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
                new Permission("MANAGE_USERS", "Create, edit, and delete users", "ADMIN"),
                new Permission("MANAGE_MENUS", "Create, edit, and delete menu items", "ADMIN"),
                new Permission("MANAGE_TAGS", "Create, merge, and delete tags", "CONTENT"),
                new Permission("MANAGE_BOOKMARKS", "Edit and delete any bookmark", "CONTENT"),
                new Permission("MANAGE_INVITATIONS", "Create and manage invitation codes", "ADMIN"),
                new Permission("MODERATE_COMMENTS", "Edit and delete any comment", "MODERATION"),
                new Permission("VIEW_AUDIT_LOG", "View audit log entries", "ADMIN"),
                new Permission("MANAGE_BACKUP", "Create and restore backups", "ADMIN"),
                new Permission("VIEW_STATS", "View system statistics", "ADMIN"),
                new Permission("COMMENT", "Post comments on bookmarks", "COMMUNITY"),
                new Permission("VOTE", "Vote on comments", "COMMUNITY")
        );
        permissionRepository.saveAll(permissions);

        // Default permission assignments
        // ADMIN: all except implicit SUPER_ADMIN-only features
        assignPermissions(Role.ADMIN, Arrays.asList(
                "MANAGE_USERS", "MANAGE_MENUS", "MANAGE_TAGS", "MANAGE_BOOKMARKS",
                "MANAGE_INVITATIONS", "MODERATE_COMMENTS", "VIEW_AUDIT_LOG",
                "MANAGE_BACKUP", "VIEW_STATS", "COMMENT", "VOTE"
        ));

        // MODERATOR: limited admin + community
        assignPermissions(Role.MODERATOR, Arrays.asList(
                "MANAGE_TAGS", "MODERATE_COMMENTS", "VIEW_AUDIT_LOG",
                "VIEW_STATS", "COMMENT", "VOTE"
        ));

        // USER: community only
        assignPermissions(Role.USER, Arrays.asList("COMMENT", "VOTE"));

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
                .role(Role.USER)
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

        log.info("Default users created: admin/admin123 (SUPER_ADMIN), user/user123 (USER)");
    }

    private void initMenuItems() {
        if (menuItemRepository.count() > 0) {
            return;
        }

        log.info("Initializing menu items...");

        // User Sidebar menu items
        menuItemRepository.save(MenuItem.builder().label("All Bookmarks").url("/").menuType(MenuType.SIDEBAR)
                .displayOrder(1).visible(true).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Import").url("/import").menuType(MenuType.SIDEBAR)
                .displayOrder(2).visible(true).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Admin Panel").url("/admin").menuType(MenuType.SIDEBAR)
                .displayOrder(3).visible(true).requiredRole(Role.MODERATOR).systemItem(true).createdBy("system").build());

        // Admin Sidebar menu items
        menuItemRepository.save(MenuItem.builder().label("Dashboard").url("/admin").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(1).visible(true).requiredPermission("VIEW_STATS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Users").url("/admin/users").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(2).visible(true).requiredPermission("MANAGE_USERS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("All Bookmarks").url("/admin/bookmarks").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(3).visible(true).requiredPermission("MANAGE_BOOKMARKS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Tag Management").url("/admin/tags").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(4).visible(true).requiredPermission("MANAGE_TAGS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Audit Log").url("/admin/audit").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(6).visible(true).requiredPermission("VIEW_AUDIT_LOG").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Menu Management").url("/admin/menus").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(7).visible(true).requiredPermission("MANAGE_MENUS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Permissions").url("/admin/permissions").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(8).visible(true).requiredRole(Role.SUPER_ADMIN).systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Comments").url("/admin/comments").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(5).visible(true).requiredPermission("MODERATE_COMMENTS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Invitations").url("/admin/invitations").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(5).visible(true).requiredPermission("MANAGE_INVITATIONS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Backup / Restore").url("/admin/backup").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(9).visible(true).requiredPermission("MANAGE_BACKUP").systemItem(true).createdBy("system").build());

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
                .maxUses(10)
                .active(true)
                .note("Default test invitation code")
                .assignedRole(Role.USER)
                .build();
        invitationCodeRepository.save(code);

        log.info("Default invitation code created: WELCOME1");
    }
}

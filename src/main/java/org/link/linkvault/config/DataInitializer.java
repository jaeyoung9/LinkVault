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
    private final QnaArticleRepository qnaArticleRepository;
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
            initQnaArticles();
        } else {
            initBootstrapAdmin();
        }
    }

    private void initQnaArticles() {
        if (qnaArticleRepository.count() > 0) {
            return;
        }

        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            log.warn("Admin user not found, skipping QnA initialization");
            return;
        }

        log.info("Initializing user guide QnA articles...");

        String quickStartAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Quick Start for New Users</p>\n" +
                "<p style=\"margin-bottom: 8px;\">Follow these steps to start using LinkVault effectively:</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li><strong>Open Feed</strong> at <a href=\"/\">/</a> to check your bookmark timeline.</li>\n" +
                "  <li><strong>Create folders</strong> for core topics (Work, Study, Tools, Inspiration).</li>\n" +
                "  <li><strong>Add bookmarks</strong> with a clear title, URL, and tags.</li>\n" +
                "  <li><strong>Use Saved &amp; Private</strong> at <a href=\"/saved\">/saved</a> for personal curation.</li>\n" +
                "  <li><strong>Search quickly</strong> when you need to find links by keyword.</li>\n" +
                "</ol>\n" +
                "<p style=\"margin-top: 10px;\"><strong>Tip:</strong> Keep your first folder structure simple, then refine it weekly.</p>";

        String screensGuideAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Main Screens and Best Use Cases</p>\n" +
                "<table style=\"width: 100%; border-collapse: collapse;\">\n" +
                "  <thead>\n" +
                "    <tr>\n" +
                "      <th style=\"text-align:left; border-bottom:1px solid #ddd; padding:8px;\">Screen</th>\n" +
                "      <th style=\"text-align:left; border-bottom:1px solid #ddd; padding:8px;\">What to do there</th>\n" +
                "      <th style=\"text-align:left; border-bottom:1px solid #ddd; padding:8px;\">Open</th>\n" +
                "    </tr>\n" +
                "  </thead>\n" +
                "  <tbody>\n" +
                "    <tr><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Feed</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Review recent bookmarks and updates.</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\"><a href=\"/\">Go</a></td></tr>\n" +
                "    <tr><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Map Discover</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Explore location-based discoveries.</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\"><a href=\"/map\">Go</a></td></tr>\n" +
                "    <tr><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Saved &amp; Private</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Manage private and saved links.</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\"><a href=\"/saved\">Go</a></td></tr>\n" +
                "    <tr><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Import</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\">Import bookmarks from external files.</td><td style=\"padding:8px; border-bottom:1px solid #f0f0f0;\"><a href=\"/import\">Go</a></td></tr>\n" +
                "    <tr><td style=\"padding:8px;\">Settings</td><td style=\"padding:8px;\">Customize account and preferences.</td><td style=\"padding:8px;\"><a href=\"/settings\">Go</a></td></tr>\n" +
                "  </tbody>\n" +
                "</table>";

        String organizationAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Organize Bookmarks with Folders and Tags</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li>Create top-level folders by domain (Product, Engineering, Design).</li>\n" +
                "  <li>Add tags for cross-cutting context (API, UX, Docs, Security).</li>\n" +
                "  <li>Use folders for structure and tags for searchability.</li>\n" +
                "  <li>Run a weekly cleanup: merge duplicates and move misplaced links.</li>\n" +
                "</ol>\n" +
                "<p style=\"margin-top: 10px;\"><strong>Good tag examples:</strong> <code>backend</code>, <code>spring</code>, <code>career</code>, <code>design-system</code>.</p>";

        String composeModalGuideAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Share a Story Modal - Step by Step Guide</p>\n" +
                "<p style=\"margin-bottom: 8px;\">This guide follows the actual compose modal UI and explains the best user flow.</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li><strong>Title:</strong> Enter a clear post title in <code>#bmTitle</code>.</li>\n" +
                "  <li><strong>Location:</strong> Click map to place a marker or use location search.</li>\n" +
                "  <li><strong>Marker Emoji:</strong> Pick an emoji to personalize your map pin.</li>\n" +
                "  <li><strong>Photos:</strong> Drag and drop up to 4 photos.</li>\n" +
                "  <li><strong>Caption + Link:</strong> Add your story and optional URL.</li>\n" +
                "  <li><strong>Tags + Folder:</strong> Classify content for long-term retrieval.</li>\n" +
                "  <li><strong>Private option:</strong> Enable private mode when needed.</li>\n" +
                "  <li><strong>Share:</strong> Click <em>Share</em> to publish.</li>\n" +
                "</ol>\n" +
                "<p style=\"font-weight: 600; margin-top: 14px; margin-bottom: 8px;\">Live UI Design Snippet (from your screen)</p>\n" +
                "<div class=\"modal post-compose-modal\">\n" +
                "  <h3 id=\"modalTitle\">Share a Story</h3>\n" +
                "  <form id=\"bookmarkForm\">\n" +
                "    <div class=\"form-group\"><label for=\"bmTitle\">Title *</label><input type=\"text\" id=\"bmTitle\" class=\"form-control\" placeholder=\"Give your story a title\"/></div>\n" +
                "    <div class=\"form-group\"><label>Location <span style=\"font-size:0.75rem;color:var(--text-muted);\">(click map to place marker)</span></label><div id=\"composeMap\" class=\"compose-map\"></div><input type=\"text\" id=\"bmLocationSearch\" class=\"form-control\" placeholder=\"Search address...\" style=\"margin-top:6px;\"/></div>\n" +
                "    <div class=\"form-group\"><label>Map Marker Emoji</label><div class=\"emoji-picker-row\"><button type=\"button\" class=\"btn btn-outline emoji-picker-selected\">&#128205;</button><button type=\"button\" class=\"btn btn-sm btn-outline\">Clear</button></div></div>\n" +
                "    <div class=\"form-group\"><label>Photos <span style=\"font-size:0.75rem;color:var(--text-muted);\">(max 4)</span></label><div id=\"photoDropZone\" class=\"photo-drop-zone\"><p>Drop photos here or click to browse</p></div></div>\n" +
                "    <div class=\"form-group\"><label for=\"bmCaption\">Caption / Story</label><textarea id=\"bmCaption\" class=\"form-control\" rows=\"3\" placeholder=\"Share your experience...\"></textarea></div>\n" +
                "    <div class=\"form-group\"><label for=\"bmUrl\">Link <span style=\"font-size:0.75rem;color:var(--text-muted);\">(optional)</span></label><input type=\"url\" id=\"bmUrl\" class=\"form-control\" placeholder=\"https://...\"/></div>\n" +
                "    <div class=\"form-group\" style=\"display:flex;align-items:center;gap:8px;\"><input type=\"checkbox\" id=\"bmPrivate\" style=\"width:auto;\"/><label for=\"bmPrivate\" style=\"margin:0;font-size:0.85rem;color:var(--text-secondary);\">Private (only visible to me)</label></div>\n" +
                "    <div class=\"form-actions\"><button type=\"button\" class=\"btn btn-outline\">Cancel</button><button type=\"submit\" class=\"btn btn-primary\">Share</button></div>\n" +
                "  </form>\n" +
                "</div>\n" +
                "<p style=\"margin-top: 10px;\"><strong>Pro tip:</strong> Write title first, then location, then media. This gives the best post quality and discoverability.</p>";

        String feedPageGuideAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Feed Page Guide (/) - Step by Step</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li>Use <strong>+ Share a Story</strong> to add a new post.</li>\n" +
                "  <li>Scroll cards to review title, tags, and community activity.</li>\n" +
                "  <li>Open a card to read details, comments, and related links.</li>\n" +
                "  <li>Use sidebar folders/tags for quick filtering.</li>\n" +
                "</ol>\n" +
                "<div class=\"header-bar\"><h2>Feed</h2><button class=\"btn btn-primary\">+ Share a Story</button></div>\n" +
                "<p style=\"margin-top: 10px;\">Best for: daily review and fast posting.</p>";

        String mapPageGuideAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Map Discover Guide (/map)</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li>Open Map Discover and zoom to your area of interest.</li>\n" +
                "  <li>Click markers to inspect stories and attached media.</li>\n" +
                "  <li>Create a new story with location to improve map quality.</li>\n" +
                "</ol>\n" +
                "<div class=\"header-bar\"><h2>Map Discover</h2><button class=\"btn btn-primary\">+ Share a Story</button></div>\n" +
                "<div id=\"map\" style=\"height:220px;border-radius:10px;border:1px solid #e5e7eb;display:flex;align-items:center;justify-content:center;color:#6b7280;\">Map area preview</div>";

        String savedPageGuideAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Saved &amp; Private Guide (/saved)</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li>Check <strong>Saved</strong> items you bookmarked from others.</li>\n" +
                "  <li>Manage your <strong>Private</strong> list that only you can see.</li>\n" +
                "  <li>Move important items into folders for long-term structure.</li>\n" +
                "</ol>\n" +
                "<div class=\"header-bar\"><h2>Saved &amp; Private</h2></div>\n" +
                "<div class=\"bookmark-grid\"><div class=\"bookmark-card\">Saved item example</div><div class=\"bookmark-card\">Private item example</div></div>";

        String searchPageGuideAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Search Guide (/search)</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li>Type focused keywords (topic/tool/tag) in search.</li>\n" +
                "  <li>Refine using folder/tag context in result cards.</li>\n" +
                "  <li>Reuse high-value tags from top results for consistency.</li>\n" +
                "</ol>\n" +
                "<form class=\"search-form\"><input class=\"search-input\" placeholder=\"Search posts, tags, folders...\"/><button class=\"btn btn-primary\" type=\"button\">Search</button></form>";

        String qnaGuideAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">QnA Page Guide (/qna)</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.7;\">\n" +
                "  <li>Use category chips to narrow down topics quickly.</li>\n" +
                "  <li>Use keyword search for exact workflows.</li>\n" +
                "  <li>Open article detail and give helpful feedback.</li>\n" +
                "</ol>\n" +
                "<div class=\"qna-categories\"><span class=\"qna-category-chip active\">All</span><span class=\"qna-category-chip\">Getting Started</span><span class=\"qna-category-chip\">Screens</span></div>\n" +
                "<div class=\"qna-article-list\"><a class=\"qna-article-card\"><span class=\"qna-article-question\">How should I start using LinkVault?</span></a></div>";

        String dailyRoutineAnswer = "<p style=\"font-weight: 600; margin-bottom: 8px;\">Daily LinkVault Routine (10 Minutes)</p>\n" +
                "<ol style=\"padding-left: 18px; line-height: 1.8;\">\n" +
                "  <li><strong>Morning:</strong> Review <a href=\"/\">Feed</a> for latest links.</li>\n" +
                "  <li><strong>During work:</strong> Save useful links immediately with tags.</li>\n" +
                "  <li><strong>Afternoon:</strong> Move important links into the right folders.</li>\n" +
                "  <li><strong>End of day:</strong> Remove duplicates and low-value items.</li>\n" +
                "  <li><strong>Weekly:</strong> Use <a href=\"/search?keyword=\">Search</a> to audit tag consistency.</li>\n" +
                "</ol>\n" +
                "<p style=\"margin-top: 10px;\">Consistency keeps your vault useful and searchable.</p>";

        List<QnaArticle> articles = Arrays.asList(
                QnaArticle.builder().question("How should I start using LinkVault as a new user?").answer(quickStartAnswer)
                        .category("Getting Started").tags("onboarding,quick-start,beginner").displayOrder(1)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/\n/saved").createdBy(admin).build(),
                QnaArticle.builder().question("What does each main screen do, and when should I use it?").answer(screensGuideAnswer)
                        .category("Screens").tags("feed,map,saved,import,settings").displayOrder(2)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/map\n/saved\n/import\n/settings").createdBy(admin).build(),
                QnaArticle.builder().question("How can I organize bookmarks effectively with folders and tags?").answer(organizationAnswer)
                        .category("Bookmark Management").tags("folders,tags,organization,workflow").displayOrder(3)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/\n/qna").createdBy(admin).build(),
                QnaArticle.builder().question("How do I use the Share a Story modal step by step, with real UI HTML?").answer(composeModalGuideAnswer)
                        .category("Create Flow").tags("html,compose,share-story,guide").displayOrder(4)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/\n/map\n/saved").createdBy(admin).build(),
                QnaArticle.builder().question("How do I use the Feed page effectively?").answer(feedPageGuideAnswer)
                        .category("Screens").tags("feed,workflow,home").displayOrder(5)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/\n/bookmark/1").createdBy(admin).build(),
                QnaArticle.builder().question("How do I use Map Discover to explore stories?").answer(mapPageGuideAnswer)
                        .category("Screens").tags("map,location,discover").displayOrder(6)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/map").createdBy(admin).build(),
                QnaArticle.builder().question("How should I manage Saved & Private content?").answer(savedPageGuideAnswer)
                        .category("Screens").tags("saved,private,curation").displayOrder(7)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/saved").createdBy(admin).build(),
                QnaArticle.builder().question("How can I search faster and get better results?").answer(searchPageGuideAnswer)
                        .category("Screens").tags("search,keyword,tags").displayOrder(8)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/").createdBy(admin).build(),
                QnaArticle.builder().question("How should I navigate and use the QnA page itself?").answer(qnaGuideAnswer)
                        .category("Screens").tags("qna,categories,help").displayOrder(9)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/qna").createdBy(admin).build(),
                QnaArticle.builder().question("What is the best step-by-step routine for daily LinkVault usage?").answer(dailyRoutineAnswer)
                        .category("Best Practices").tags("daily-routine,productivity,best-practices").displayOrder(10)
                        .status(QnaStatus.PUBLISHED).relatedLinks("/\n/saved").createdBy(admin).build()
        );

        qnaArticleRepository.saveAll(articles);
        log.info("User guide QnA articles initialized: {}", articles.size());
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
                new Permission("SYSTEM_SETTINGS", "Manage system settings", "ADMIN"),
                new Permission("VIEW_MONETIZATION", "View monetization dashboard", "ADMIN"),
                new Permission("MANAGE_PAYMENTS", "Manage payments and refunds", "ADMIN")
        );
        permissionRepository.saveAll(permissions);

        // Default permission assignments
        // COMMUNITY_ADMIN: all except CONTENT_PURGE (reserved for SUPER_ADMIN)
        assignPermissions(Role.COMMUNITY_ADMIN, Arrays.asList(
                "USER_MANAGE", "MENU_MANAGE", "INVITE_ISSUE", "INVITE_REVOKE",
                "AUDIT_VIEW", "BACKUP_RUN", "VIEW_STATS", "MANAGE_ANNOUNCEMENTS",
                "MANAGE_TAGS", "MANAGE_BOOKMARKS", "BOOKMARK_DELETE_ANY", "BOOKMARK_RESTORE",
                "MANAGE_QNA", "COMMENT_HIDE", "COMMENT_RESTORE",
                "COMMENT", "VOTE", "SYSTEM_SETTINGS",
                "VIEW_MONETIZATION", "MANAGE_PAYMENTS"
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
        menuItemRepository.save(MenuItem.builder().label("Tag Management").url("/admin/tags").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(3).visible(true).requiredPermission("MANAGE_TAGS").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("All Bookmarks").url("/admin/bookmarks").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(4).visible(true).requiredPermission("MANAGE_BOOKMARKS").systemItem(true).createdBy("system").build());
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
        menuItemRepository.save(MenuItem.builder().label("Monetization").url("/admin/monetization").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(15).visible(true).requiredPermission("VIEW_MONETIZATION").systemItem(true).createdBy("system").build());
        menuItemRepository.save(MenuItem.builder().label("Guest Analytics").url("/admin/guest-analytics").menuType(MenuType.ADMIN_SIDEBAR)
                .displayOrder(16).visible(true).requiredPermission("VIEW_MONETIZATION").systemItem(true).createdBy("system").build());

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

        // Audit policy settings
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("audit.retention.enabled")
                .settingValue("false")
                .description("Enable automatic audit log retention cleanup")
                .category("AUDIT_POLICY")
                .build());

        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("audit.retention.days")
                .settingValue("365")
                .description("Days to retain audit log entries (30-3650)")
                .category("AUDIT_POLICY")
                .build());

        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("audit.delete.mode")
                .settingValue("SOFT")
                .description("Retention delete mode: SOFT (archive) or HARD (permanent)")
                .category("AUDIT_POLICY")
                .build());

        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("audit.masking.level")
                .settingValue("BASIC")
                .description("Audit detail masking level: NONE, BASIC, or STRICT")
                .category("AUDIT_POLICY")
                .build());

        log.info("System settings initialized with defaults");

        // Monetization settings
        initMonetizationSettings();
    }

    private void initMonetizationSettings() {
        log.info("Initializing monetization settings...");

        // Feature flags (all OFF by default)
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("feature.guest-access-enabled").settingValue("true")
                .description("Enable anonymous guest read access").category("FEATURE_FLAGS").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("feature.ads-enabled").settingValue("false")
                .description("Enable ad display globally").category("FEATURE_FLAGS").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("feature.rewarded-video-enabled").settingValue("false")
                .description("Enable rewarded video system").category("FEATURE_FLAGS").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("feature.donations-enabled").settingValue("false")
                .description("Enable donation section").category("FEATURE_FLAGS").build());

        // Ad policy
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.max-per-page").settingValue("3")
                .description("Max ad cards per page load").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.max-per-session").settingValue("10")
                .description("Max ads per session").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.max-per-hour").settingValue("15")
                .description("Max ads per hour").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.feed-insertion-interval").settingValue("6")
                .description("Insert 1 ad every N posts").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.guest-frequency-multiplier").settingValue("1.5")
                .description("Guest ad frequency multiplier vs members").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.guest-first-session-grace").settingValue("3")
                .description("Page views before first guest ad").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.adsense-client-id").settingValue("")
                .description("Google AdSense client ID (ca-pub-xxx)").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.adsense-slot-feed").settingValue("")
                .description("AdSense slot ID for feed native ads (1234567890)").category("AD_POLICY").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("ad.adsense-layout-key").settingValue("")
                .description("AdSense in-feed ad layout key (e.g. -ef+6k-30-ac+ty)").category("AD_POLICY").build());

        // Reward settings
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("reward.video-points").settingValue("10")
                .description("Points per rewarded video").category("REWARD").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("reward.adfree-hours-cost").settingValue("50")
                .description("Points to redeem ad-free hours").category("REWARD").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("reward.adfree-hours-duration").settingValue("2")
                .description("Hours per redemption").category("REWARD").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("reward.daily-video-cap").settingValue("5")
                .description("Max videos per day").category("REWARD").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("reward.ima-ad-tag-url").settingValue("https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&correlator=")
                .description("IMA SDK ad tag URL").category("REWARD").build());

        // Stripe settings
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("stripe.api-key-publishable").settingValue("")
                .description("Stripe publishable key (pk_test_...)").category("STRIPE").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("stripe.api-key-secret").settingValue("")
                .description("Stripe secret key (sk_test_...)").category("STRIPE").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("stripe.webhook-secret").settingValue("")
                .description("Stripe webhook signing secret (whsec_...)").category("STRIPE").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("stripe.adfree-7d-price-id").settingValue("")
                .description("Stripe Price ID for 7-day ad-free pass").category("STRIPE").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("stripe.adfree-30d-price-id").settingValue("")
                .description("Stripe Price ID for 30-day ad-free pass").category("STRIPE").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("stripe.currency").settingValue("usd")
                .description("Default currency for Stripe payments").category("STRIPE").build());

        // Donation settings
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("donation.one-time-amounts").settingValue("5,10,25,50")
                .description("Suggested one-time donation amounts (USD)").category("DONATION").build());
        systemSettingsRepository.save(SystemSettings.builder()
                .settingKey("donation.recurring-amounts").settingValue("3,5,10")
                .description("Suggested monthly recurring amounts (USD)").category("DONATION").build());

        log.info("Monetization settings initialized (all feature flags OFF)");
    }
}

package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.dto.FolderResponseDto;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.link.linkvault.entity.Role;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final BookmarkService bookmarkService;
    private final FolderService folderService;
    private final TagService tagService;
    private final UserService userService;
    private final SavedBookmarkService savedBookmarkService;
    private final QnaArticleService qnaArticleService;
    private final AnnouncementService announcementService;
    private final UserSettingsService userSettingsService;
    private final PrivacyPolicyService privacyPolicyService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/privacy-consent")
    public String privacyConsent(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        PrivacyPolicyResponseDto policy = privacyPolicyService.getActivePolicy();

        // Already consented → redirect to home
        if (policy != null && currentUser.getPrivacyAgreedVersion() != null
                && currentUser.getPrivacyAgreedVersion() == policy.getVersion()) {
            return "redirect:/";
        }

        if (policy != null) {
            model.addAttribute("policyContent", policy.getContent());
            model.addAttribute("policyVersion", policy.getVersion());
        }
        return "privacy-consent";
    }

    @GetMapping("/register")
    public String register(Model model) {
        PrivacyPolicyResponseDto policy = privacyPolicyService.getActivePolicy();
        if (policy != null) {
            model.addAttribute("privacyPolicyContent", policy.getContent());
            model.addAttribute("privacyPolicyVersion", policy.getVersion());
        }
        return "register";
    }

    @GetMapping("/")
    public String home(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        Page<BookmarkResponseDto> bookmarks = bookmarkService.findAll(currentUser,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        populateCommonModel(model, currentUser);
        model.addAttribute("bookmarks", bookmarks);
        model.addAttribute("pageTitle", "Feed");
        model.addAttribute("frequent", bookmarkService.findFrequentlyAccessed(currentUser, 5));
        return "index";
    }

    @GetMapping("/folder/{id}")
    public String folderView(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id, Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        List<BookmarkResponseDto> bookmarks = bookmarkService.findByFolderId(currentUser, id);
        FolderResponseDto folder = folderService.findById(currentUser, id);
        populateCommonModel(model, currentUser);
        model.addAttribute("bookmarkList", bookmarks);
        model.addAttribute("currentFolder", folder);
        model.addAttribute("pageTitle", "Folder: " + folder.getName());
        return "folder";
    }

    @GetMapping("/tag/{tagName}")
    public String tagView(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String tagName, Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        List<BookmarkResponseDto> bookmarks = bookmarkService.findByTagName(currentUser, tagName);
        populateCommonModel(model, currentUser);
        model.addAttribute("bookmarkList", bookmarks);
        model.addAttribute("currentTag", tagName);
        model.addAttribute("pageTitle", "Tag: " + tagName);
        return "tag";
    }

    @GetMapping("/search")
    public String search(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        Page<BookmarkResponseDto> results = bookmarkService.searchByKeyword(currentUser, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        populateCommonModel(model, currentUser);
        model.addAttribute("bookmarks", results);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Search: " + keyword);
        return "search";
    }

    @GetMapping("/bookmark/{id}")
    public String bookmarkDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id, Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        BookmarkResponseDto bookmark = bookmarkService.findById(id, currentUser);

        populateCommonModel(model, currentUser);
        model.addAttribute("bookmark", bookmark);
        model.addAttribute("pageTitle", bookmark.getTitle());

        model.addAttribute("currentUserId", currentUser.getId());
        return "bookmark";
    }

    @GetMapping("/map")
    public String mapDiscovery(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("pageTitle", "Map Discover");
        return "map";
    }

    @GetMapping("/saved")
    public String savedPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("savedList", savedBookmarkService.getSavedBookmarks(currentUser));
        model.addAttribute("privateList", bookmarkService.findPrivateByUser(currentUser));
        model.addAttribute("pageTitle", "Saved & Private");
        return "saved";
    }

    @GetMapping("/import")
    public String importPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("pageTitle", "Import Bookmarks");
        return "import";
    }

    @GetMapping("/qna")
    public String qna(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("categories", qnaArticleService.getPublishedCategories());
        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("articles", qnaArticleService.searchPublished(keyword, PageRequest.of(0, 50)).getContent());
            model.addAttribute("keyword", keyword);
        } else if (category != null && !category.isBlank()) {
            model.addAttribute("articles", qnaArticleService.findPublishedByCategory(category));
            model.addAttribute("selectedCategory", category);
        } else {
            model.addAttribute("articles", qnaArticleService.findAllPublished());
        }
        model.addAttribute("pageTitle", "QnA Guide");
        return "qna";
    }

    @GetMapping("/qna/{id}")
    public String qnaDetail(@PathVariable Long id, Model model,
                            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("article", qnaArticleService.findPublishedById(id));
        model.addAttribute("feedback", qnaArticleService.getFeedback(id, currentUser));
        model.addAttribute("pageTitle", "QnA Guide");
        return "qna-detail";
    }

    @GetMapping("/announcements")
    public String announcements(Model model,
                                @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("announcements", announcementService.findVisibleForUser(currentUser));
        model.addAttribute("pageTitle", "Announcements");
        return "announcements";
    }

    @GetMapping("/announcements/{id}")
    public String announcementDetail(@PathVariable Long id, Model model,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        announcementService.markAsRead(id, currentUser);
        model.addAttribute("announcement", announcementService.findById(id, currentUser));
        model.addAttribute("pageTitle", "Announcement");
        return "announcement-detail";
    }

    @GetMapping("/settings")
    public String settings(Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("settings", userSettingsService.getSettings(currentUser));
        model.addAttribute("userEmail", currentUser.getEmail());
        model.addAttribute("pageTitle", "Settings");
        return "settings";
    }

    private void populateCommonModel(Model model, User currentUser) {
        model.addAttribute("folders", folderService.findRootFolders(currentUser));
        model.addAttribute("tags", tagService.findAll());
        model.addAttribute("currentUser", currentUser.getUsername());
        boolean isAdmin = currentUser.getRole() == Role.SUPER_ADMIN
                || currentUser.getRole() == Role.COMMUNITY_ADMIN
                || currentUser.getRole() == Role.MODERATOR;
        model.addAttribute("isAdmin", isAdmin);
    }
}

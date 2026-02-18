package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.dto.FolderResponseDto;
import org.link.linkvault.dto.PrivacyPolicyResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.util.Collections;
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
    private final SystemSettingsService systemSettingsService;
    private final AdPolicyService adPolicyService;

    private boolean isGuestAccessEnabled() {
        return systemSettingsService.getValue("feature.guest-access-enabled")
                .map("true"::equals).orElse(false);
    }

    private void populateGuestModel(Model model) {
        model.addAttribute("folders", Collections.emptyList());
        model.addAttribute("tags", tagService.findAll());
        model.addAttribute("currentUser", null);
        model.addAttribute("isAdmin", false);
        model.addAttribute("isGuest", true);
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("guestAccessEnabled", isGuestAccessEnabled());
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            Page<BookmarkResponseDto> bookmarks = bookmarkService.findAllPublic(pageable);
            populateGuestModel(model);
            populateAdModel(model, null, bookmarks.getNumberOfElements());
            model.addAttribute("bookmarks", bookmarks);
            model.addAttribute("pageTitle", "Feed");
            model.addAttribute("frequent", Collections.emptyList());
            return "index";
        }

        User currentUser = userService.getUserEntity(userDetails.getUsername());
        Page<BookmarkResponseDto> bookmarks = bookmarkService.findAll(currentUser, pageable);
        populateCommonModel(model, currentUser);
        populateAdModel(model, currentUser, bookmarks.getNumberOfElements());
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
        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            List<BookmarkResponseDto> bookmarks = bookmarkService.findByTagNamePublic(tagName);
            populateGuestModel(model);
            model.addAttribute("bookmarkList", bookmarks);
            model.addAttribute("currentTag", tagName);
            model.addAttribute("pageTitle", "Tag: " + tagName);
            return "tag";
        }

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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            Page<BookmarkResponseDto> results = bookmarkService.searchByKeywordPublic(keyword, pageable);
            populateGuestModel(model);
            model.addAttribute("bookmarks", results);
            model.addAttribute("keyword", keyword);
            model.addAttribute("pageTitle", "Search: " + keyword);
            return "search";
        }

        User currentUser = userService.getUserEntity(userDetails.getUsername());
        Page<BookmarkResponseDto> results = bookmarkService.searchByKeyword(currentUser, keyword, pageable);
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
        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            BookmarkResponseDto bookmark = bookmarkService.findByIdPublic(id);
            populateGuestModel(model);
            model.addAttribute("bookmark", bookmark);
            model.addAttribute("pageTitle", bookmark.getTitle());
            model.addAttribute("currentUserId", null);
            return "bookmark";
        }

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
        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            populateGuestModel(model);
            model.addAttribute("pageTitle", "Map Discover");
            return "map";
        }

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
        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            populateGuestModel(model);
        } else {
            User currentUser = userService.getUserEntity(userDetails.getUsername());
            populateCommonModel(model, currentUser);
        }

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
        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            populateGuestModel(model);
            model.addAttribute("article", qnaArticleService.findPublishedById(id));
            model.addAttribute("feedback", null);
            model.addAttribute("pageTitle", "QnA Guide");
            return "qna-detail";
        }

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
        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            populateGuestModel(model);
            model.addAttribute("announcements", Collections.emptyList());
            model.addAttribute("pageTitle", "Announcements");
            return "announcements";
        }

        User currentUser = userService.getUserEntity(userDetails.getUsername());
        populateCommonModel(model, currentUser);
        model.addAttribute("announcements", announcementService.findVisibleForUser(currentUser));
        model.addAttribute("pageTitle", "Announcements");
        return "announcements";
    }

    @GetMapping("/announcements/{id}")
    public String announcementDetail(@PathVariable Long id, Model model,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            if (!isGuestAccessEnabled()) return "redirect:/login";
            populateGuestModel(model);
            model.addAttribute("announcement", null);
            model.addAttribute("pageTitle", "Announcement");
            return "announcement-detail";
        }

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

        // Donation amounts from SystemSettings (comma-separated dollar values)
        String oneTimeRaw = systemSettingsService.getValue("donation.one-time-amounts").orElse("5,10,25,50");
        String recurringRaw = systemSettingsService.getValue("donation.recurring-amounts").orElse("3,5,10");
        model.addAttribute("donationOneTimeAmounts", parseAmounts(oneTimeRaw));
        model.addAttribute("donationRecurringAmounts", parseAmounts(recurringRaw));
        return "settings";
    }

    private List<Integer> parseAmounts(String csv) {
        List<Integer> amounts = new java.util.ArrayList<>();
        for (String s : csv.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                try {
                    amounts.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return amounts;
    }

    private void populateAdModel(Model model, User currentUser, int totalPosts) {
        boolean adsEnabled = adPolicyService.isAdsEnabled();
        boolean isAdFree = currentUser != null && adPolicyService.isAdFree(currentUser);
        boolean isGuest = currentUser == null;
        model.addAttribute("adsEnabled", adsEnabled && !isAdFree);
        model.addAttribute("isAdFree", isAdFree);
        model.addAttribute("adsenseClientId", adPolicyService.getAdsenseClientId());
        model.addAttribute("adsenseSlotFeed", adPolicyService.getAdsenseSlotFeed());
        model.addAttribute("adsenseLayoutKey", adPolicyService.getAdsenseLayoutKey());
        if (adsEnabled && !isAdFree) {
            model.addAttribute("adPositions", adPolicyService.getAdInsertionPositions(totalPosts, isGuest, 0));
        } else {
            model.addAttribute("adPositions", Collections.emptyList());
        }
    }

    private void populateCommonModel(Model model, User currentUser) {
        model.addAttribute("folders", folderService.findRootFolders(currentUser));
        model.addAttribute("tags", tagService.findAll());
        model.addAttribute("currentUser", currentUser.getUsername());
        boolean isAdmin = currentUser.getRole() == Role.SUPER_ADMIN
                || currentUser.getRole() == Role.COMMUNITY_ADMIN
                || currentUser.getRole() == Role.MODERATOR;
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isGuest", false);
    }
}

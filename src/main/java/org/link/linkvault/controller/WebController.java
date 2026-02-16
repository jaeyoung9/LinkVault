package org.link.linkvault.controller;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.BookmarkResponseDto;
import org.link.linkvault.dto.FolderResponseDto;
import org.link.linkvault.entity.User;
import org.link.linkvault.service.BookmarkService;
import org.link.linkvault.service.FolderService;
import org.link.linkvault.service.TagService;
import org.link.linkvault.service.UserService;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final BookmarkService bookmarkService;
    private final FolderService folderService;
    private final TagService tagService;
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
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
        model.addAttribute("pageTitle", "All Bookmarks");
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
        BookmarkResponseDto bookmark = bookmarkService.findById(id);
        populateCommonModel(model, currentUser);
        model.addAttribute("bookmark", bookmark);
        model.addAttribute("pageTitle", bookmark.getTitle());

        model.addAttribute("currentUserId", currentUser.getId());
        return "bookmark";
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

    private void populateCommonModel(Model model, User currentUser) {
        model.addAttribute("folders", folderService.findRootFolders(currentUser));
        model.addAttribute("tags", tagService.findAll());
        model.addAttribute("currentUser", currentUser.getUsername());
    }
}

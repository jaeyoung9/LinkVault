package org.link.linkvault.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.link.linkvault.entity.Bookmark;
import org.link.linkvault.entity.Folder;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.BookmarkRepository;
import org.link.linkvault.repository.FolderRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FolderRepository folderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Initializing default users...");

        User admin = User.builder()
                .username("admin")
                .email("admin@linkvault.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
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

        log.info("Default users created: admin/admin123 (ADMIN), user/user123 (USER)");
    }
}

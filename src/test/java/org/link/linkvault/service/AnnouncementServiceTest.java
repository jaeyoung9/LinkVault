package org.link.linkvault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.link.linkvault.dto.AnnouncementRequestDto;
import org.link.linkvault.dto.AnnouncementResponseDto;
import org.link.linkvault.entity.*;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.AnnouncementRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AnnouncementServiceTest {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .username("testadmin")
                .email("testadmin@test.com")
                .password("encoded")
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build());

        memberUser = userRepository.save(User.builder()
                .username("testmember")
                .email("testmember@test.com")
                .password("encoded")
                .role(Role.MEMBER)
                .enabled(true)
                .build());
    }

    private Announcement createAnnouncement(String title, AnnouncementStatus status,
                                             Role targetRole, boolean enableComments, boolean enableVoting) {
        return announcementRepository.save(Announcement.builder()
                .title(title)
                .content("Content for " + title)
                .priority(AnnouncementPriority.INFO)
                .status(status)
                .targetRole(targetRole)
                .pinned(false)
                .enableComments(enableComments)
                .enableVoting(enableVoting)
                .createdBy(adminUser)
                .build());
    }

    @Test
    void findVisibleForGuest_returnsOnlyNullTargetRole() {
        createAnnouncement("For All", AnnouncementStatus.PUBLISHED, null, false, false);
        createAnnouncement("For Members", AnnouncementStatus.PUBLISHED, Role.MEMBER, false, false);
        createAnnouncement("For Admins", AnnouncementStatus.PUBLISHED, Role.SUPER_ADMIN, false, false);
        createAnnouncement("Draft", AnnouncementStatus.DRAFT, null, false, false);

        List<AnnouncementResponseDto> result = announcementService.findVisibleForGuest();

        assertEquals(1, result.size());
        assertEquals("For All", result.get(0).getTitle());
    }

    @Test
    void findByIdForGuest_throwsForTargetedAnnouncement() {
        Announcement targeted = createAnnouncement("Members Only", AnnouncementStatus.PUBLISHED,
                Role.MEMBER, false, false);

        assertThrows(ResourceNotFoundException.class,
                () -> announcementService.findByIdForGuest(targeted.getId()));
    }

    @Test
    void findByIdForGuest_returnsNullTargetAnnouncement() {
        Announcement general = createAnnouncement("General", AnnouncementStatus.PUBLISHED,
                null, false, false);

        AnnouncementResponseDto result = announcementService.findByIdForGuest(general.getId());

        assertNotNull(result);
        assertEquals("General", result.getTitle());
        assertFalse(result.isRead());
        assertFalse(result.isAcknowledged());
    }

    @Test
    void findVisibleForUser_memberSeesOwnRoleAndNull() {
        createAnnouncement("For All", AnnouncementStatus.PUBLISHED, null, false, false);
        createAnnouncement("For Members", AnnouncementStatus.PUBLISHED, Role.MEMBER, false, false);
        createAnnouncement("For Admins", AnnouncementStatus.PUBLISHED, Role.SUPER_ADMIN, false, false);

        List<AnnouncementResponseDto> result = announcementService.findVisibleForUser(memberUser);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getTitle().equals("For All")));
        assertTrue(result.stream().anyMatch(a -> a.getTitle().equals("For Members")));
        assertFalse(result.stream().anyMatch(a -> a.getTitle().equals("For Admins")));
    }

    @Test
    void findVisibleForUser_adminSeesAllPublished() {
        createAnnouncement("For All", AnnouncementStatus.PUBLISHED, null, false, false);
        createAnnouncement("For Members", AnnouncementStatus.PUBLISHED, Role.MEMBER, false, false);
        createAnnouncement("For Admins", AnnouncementStatus.PUBLISHED, Role.SUPER_ADMIN, false, false);
        createAnnouncement("Draft", AnnouncementStatus.DRAFT, null, false, false);

        List<AnnouncementResponseDto> result = announcementService.findVisibleForUser(adminUser);

        assertEquals(3, result.size());
        assertFalse(result.stream().anyMatch(a -> a.getTitle().equals("Draft")));
    }

    @Test
    void findById_adminNotBlockedByTargetRoleMismatch() {
        Announcement memberTargeted = createAnnouncement("For Members", AnnouncementStatus.PUBLISHED,
                Role.MEMBER, false, false);

        AnnouncementResponseDto result = announcementService.findById(memberTargeted.getId(), adminUser);

        assertNotNull(result);
        assertEquals("For Members", result.getTitle());
    }

    @Test
    void create_setsEnableCommentsAndVoting() {
        AnnouncementRequestDto dto = new AnnouncementRequestDto();
        dto.setTitle("Test Announcement");
        dto.setContent("Content");
        dto.setPriority(AnnouncementPriority.INFO);
        dto.setEnableComments(true);
        dto.setEnableVoting(true);

        AnnouncementResponseDto result = announcementService.create(dto, adminUser, adminUser.getUsername());

        assertTrue(result.isEnableComments());
        assertTrue(result.isEnableVoting());
    }

    @Test
    void update_togglesEnableCommentsAndVoting() {
        Announcement ann = createAnnouncement("Test", AnnouncementStatus.DRAFT, null, false, false);

        AnnouncementRequestDto dto = new AnnouncementRequestDto();
        dto.setTitle("Updated");
        dto.setContent("Updated content");
        dto.setPriority(AnnouncementPriority.INFO);
        dto.setEnableComments(true);
        dto.setEnableVoting(true);

        AnnouncementResponseDto result = announcementService.update(ann.getId(), dto, adminUser.getUsername());

        assertTrue(result.isEnableComments());
        assertTrue(result.isEnableVoting());
    }
}

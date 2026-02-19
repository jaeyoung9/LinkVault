package org.link.linkvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.link.linkvault.entity.*;
import org.link.linkvault.repository.AnnouncementRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnnouncementApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .username("apitestadmin")
                .email("apitestadmin@test.com")
                .password("encoded")
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build());
    }

    @Test
    void createAnnouncement_includesEnableCommentsAndVoting() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Test Create");
        data.put("content", "Content");
        data.put("priority", "INFO");
        data.put("enableComments", true);
        data.put("enableVoting", true);

        mockMvc.perform(post("/api/admin/announcements")
                        .with(user(adminUser.getUsername()).roles("SUPER_ADMIN").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("MANAGE_ANNOUNCEMENTS")
                        ))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enableComments").value(true))
                .andExpect(jsonPath("$.enableVoting").value(true));
    }

    @Test
    void updateAnnouncement_updatesEnableCommentsAndVoting() throws Exception {
        Announcement ann = announcementRepository.save(Announcement.builder()
                .title("Original")
                .content("Content")
                .priority(AnnouncementPriority.INFO)
                .status(AnnouncementStatus.DRAFT)
                .enableComments(false)
                .enableVoting(false)
                .createdBy(adminUser)
                .build());

        Map<String, Object> data = new HashMap<>();
        data.put("title", "Updated");
        data.put("content", "Updated Content");
        data.put("priority", "INFO");
        data.put("enableComments", true);
        data.put("enableVoting", true);

        mockMvc.perform(put("/api/admin/announcements/" + ann.getId())
                        .with(user(adminUser.getUsername()).roles("SUPER_ADMIN").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("MANAGE_ANNOUNCEMENTS")
                        ))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enableComments").value(true))
                .andExpect(jsonPath("$.enableVoting").value(true));
    }

    @Test
    void adminVisibility_seesAllPublishedAnnouncements() throws Exception {
        announcementRepository.save(Announcement.builder()
                .title("For All")
                .content("Content")
                .priority(AnnouncementPriority.INFO)
                .status(AnnouncementStatus.PUBLISHED)
                .targetRole(null)
                .createdBy(adminUser)
                .build());

        announcementRepository.save(Announcement.builder()
                .title("For Members")
                .content("Content")
                .priority(AnnouncementPriority.INFO)
                .status(AnnouncementStatus.PUBLISHED)
                .targetRole(Role.MEMBER)
                .createdBy(adminUser)
                .build());

        mockMvc.perform(get("/api/announcements")
                        .with(user(adminUser.getUsername()).roles("SUPER_ADMIN").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}

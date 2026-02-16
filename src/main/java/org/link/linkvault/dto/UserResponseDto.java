package org.link.linkvault.dto;

import lombok.Builder;
import lombok.Getter;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private long bookmarkCount;

    public static UserResponseDto from(User user, long bookmarkCount) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .bookmarkCount(bookmarkCount)
                .build();
    }
}

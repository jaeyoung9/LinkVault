package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.entity.Role;
import org.link.linkvault.entity.User;
import org.link.linkvault.repository.PermissionRepository;
import org.link.linkvault.repository.RolePermissionRepository;
import org.link.linkvault.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add role authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // SUPER_ADMIN gets all permissions implicitly
        if (user.getRole() == Role.SUPER_ADMIN) {
            permissionRepository.findAll().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p.getName())));
        } else {
            // Load permissions from RolePermission table
            List<String> permissionNames = rolePermissionRepository.findPermissionNamesByRole(user.getRole());
            permissionNames.forEach(name ->
                    authorities.add(new SimpleGrantedAuthority(name)));
        }

        boolean accountNonLocked = !user.isAccountLocked();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, accountNonLocked,
                authorities
        );
    }
}

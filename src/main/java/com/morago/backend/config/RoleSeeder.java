package com.morago.backend.config;

import com.morago.backend.entity.Role;
import com.morago.backend.entity.enumFiles.Roles;
import com.morago.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRole(Roles.ROLE_TRANSLATOR);
        seedRole(Roles.ROLE_USER);
        seedRole(Roles.ROLE_ADMIN);
    }

    private void seedRole(Roles roleName) {
        roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(roleName)
                                .build()
                ));
    }
}

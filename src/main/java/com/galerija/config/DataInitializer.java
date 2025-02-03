package com.galerija.config;

import com.galerija.entity.Role;
import com.galerija.entity.UserEntity;
import com.galerija.repository.RoleRepository;
import com.galerija.repository.UserRepository;
import com.galerija.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        categoryService.initializeDefaultCategories();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            Role userRole = new Role();
            userRole.setName(Role.ERole.ROLE_USER);
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setName(Role.ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setEmail("admin@galerija.com");
            admin.setPassword(passwordEncoder.encode("admin123"));

            Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Admin Role not found."));
            Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: User Role not found."));

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            roles.add(userRole);
            admin.setRoles(roles);

            userRepository.save(admin);
        }
    }
}

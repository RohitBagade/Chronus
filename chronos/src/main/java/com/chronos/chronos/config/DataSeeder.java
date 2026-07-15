package com.chronos.chronos.config;

import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds an admin account on first boot from chronos.admin.* config (idempotent). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository users, PasswordEncoder encoder,
                      @Value("${chronos.admin.email}") String adminEmail,
                      @Value("${chronos.admin.password}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(adminEmail)) {
            return;
        }
        users.save(User.builder()
                .name("Chronos Admin")
                .email(adminEmail)
                .passwordHash(encoder.encode(adminPassword))
                .role("ADMIN")
                .build());
        log.info("Seeded admin user: {}", adminEmail);
    }
}

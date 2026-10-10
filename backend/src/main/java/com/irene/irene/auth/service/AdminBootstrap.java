package com.irene.irene.auth.service;
import com.irene.irene.auth.model.*;
import com.irene.irene.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Optional, explicitly configured first admin. Never has a default cloud password. */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${BOOTSTRAP_ADMIN_EMAIL:}") private String email;
    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") private String password;
    public AdminBootstrap(UserRepository users, PasswordEncoder encoder) {this.users=users;this.encoder=encoder;}
    @Override public void run(String... args) {
        if(email.isBlank())return;
        if(!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || password.length()<16) throw new IllegalStateException("Admin bootstrap requires a valid email and password of at least 16 characters");
        if(users.existsByEmailIgnoreCase(email))return;
        if(users.findAll().stream().anyMatch(u->u.getRole()==Role.ADMIN))return;
        User admin=new User();admin.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));admin.setPasswordHash(encoder.encode(password));admin.setRole(Role.ADMIN);users.save(admin);
    }
}

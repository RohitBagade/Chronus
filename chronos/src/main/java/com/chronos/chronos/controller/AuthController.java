package com.chronos.chronos.controller;

import com.chronos.chronos.dto.AuthResponse;
import com.chronos.chronos.dto.LoginRequest;
import com.chronos.chronos.dto.SignupRequest;
import com.chronos.chronos.dto.UserView;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.UserRepository;
import com.chronos.chronos.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authManager;

    public AuthController(UserRepository users, PasswordEncoder encoder,
                          JwtService jwt, AuthenticationManager authManager) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.authManager = authManager;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        if (users.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "An account with this email already exists."));
        }
        User u = User.builder()
                .name(req.name())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .role("USER")
                .build();
        users.save(u);
        String token = jwt.generate(u.getEmail(), u.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.of(token, jwt.getExpirationMs(), u));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        // Throws BadCredentialsException (-> 401) if the password is wrong.
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        User u = users.findByEmail(req.email()).orElseThrow();
        String token = jwt.generate(u.getEmail(), u.getRole());
        return ResponseEntity.ok(AuthResponse.of(token, jwt.getExpirationMs(), u));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        User u = users.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(UserView.of(u));
    }
}

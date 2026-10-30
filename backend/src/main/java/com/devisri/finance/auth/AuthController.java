package com.devisri.finance.auth;

import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.devisri.finance.auth.AuthDtos.LoginRequest;
import com.devisri.finance.auth.AuthDtos.ProfileResponse;
import com.devisri.finance.auth.AuthDtos.RegisterRequest;
import com.devisri.finance.auth.AuthDtos.TokenResponse;
import com.devisri.finance.common.ApiExceptionHandler;
import com.devisri.finance.demo.DemoService;
import com.devisri.finance.security.JwtService;

import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DemoService demoService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                          DemoService demoService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.demoService = demoService;
    }

    @GetMapping("/")
    Map<String, String> health() {
        return Map.of("status", "API running");
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        users.save(new User(request.name().trim(), request.occupation(), email,
                passwordEncoder.encode(request.password())));
        return Map.of("message", "User registered successfully");
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return new TokenResponse(jwtService.createToken(user.getId()));
    }

    /** One click login for recruiters: a fresh account full of example data, no password needed. */
    @PostMapping("/demo-login")
    TokenResponse demoLogin() {
        return new TokenResponse(jwtService.createToken(demoService.resetDemoUser().getId()));
    }

    @GetMapping("/profile")
    ProfileResponse profile(@AuthenticationPrincipal String userId) {
        return users.findById(userId)
                .map(ProfileResponse::from)
                .orElseThrow(() -> ApiExceptionHandler.notFound("User not found"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

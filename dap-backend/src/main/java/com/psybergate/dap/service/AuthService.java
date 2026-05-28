package com.psybergate.dap.service;

import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.dto.LoginRequest;
import com.psybergate.dap.dto.LoginResponse;
import com.psybergate.dap.repository.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticate(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getRole());
    }
}

package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.MarkerRequest;
import com.psybergate.dap.dto.MarkerResponse;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarkerService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public MarkerService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MarkerResponse register(MarkerRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }
        AppUser user = AppUser.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.MARKER)
                .build();
        AppUser saved = appUserRepository.save(user);
        return new MarkerResponse(saved.getId(), saved.getName(), saved.getEmail());
    }

    @Transactional(readOnly = true)
    public PageResponse<MarkerResponse> listMarkers(int page, int size) {
        Page<AppUser> markers = appUserRepository.findAllByRole(Role.MARKER, PageRequest.of(page, size));
        List<MarkerResponse> content = markers.stream()
                .map(u -> new MarkerResponse(u.getId(), u.getName(), u.getEmail()))
                .toList();
        return new PageResponse<>(content, markers.getTotalElements(), markers.getTotalPages(),
                markers.getSize(), markers.getNumber());
    }
}

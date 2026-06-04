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
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

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
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarkerResponse> listMarkers(int page, int size, String search, String sortBy, String sortDir) {
        String sortField = switch (sortBy == null ? "name" : sortBy) {
            case "email" -> "email";
            case "createdAt" -> "createdAt";
            default -> "name";
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String searchParam = (search == null || search.isBlank()) ? null : search;
        Page<AppUser> markers = appUserRepository.searchMarkers(searchParam, PageRequest.of(page, size, Sort.by(direction, sortField)));
        List<MarkerResponse> content = markers.stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(content, markers.getTotalElements(), markers.getTotalPages(),
                markers.getSize(), markers.getNumber());
    }

    @Transactional
    public MarkerResponse updateMarker(UUID id, MarkerRequest request) {
        AppUser user = appUserRepository.findById(id)
                .filter(u -> u.getRole() == Role.MARKER)
                .orElseThrow(() -> new NoSuchElementException("Marker not found: " + id));
        if (!user.getEmail().equals(request.email()) && appUserRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }
        user.setName(request.name());
        user.setEmail(request.email());
        return toResponse(appUserRepository.save(user));
    }

    @Transactional
    public void deleteMarker(UUID id) {
        AppUser user = appUserRepository.findById(id)
                .filter(u -> u.getRole() == Role.MARKER)
                .orElseThrow(() -> new NoSuchElementException("Marker not found: " + id));
        appUserRepository.delete(user);
    }

    private MarkerResponse toResponse(AppUser user) {
        return new MarkerResponse(user.getId(), user.getName(), user.getEmail(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
    }
}

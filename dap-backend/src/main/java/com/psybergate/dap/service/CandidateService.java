package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.dto.CandidateResponse;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.repository.AppUserRepository;
import com.psybergate.dap.repository.CandidateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CandidateService {

    private final AppUserRepository appUserRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    public CandidateService(AppUserRepository appUserRepository,
                            CandidateRepository candidateRepository,
                            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CandidateResponse register(CandidateRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }

        AppUser user = AppUser.builder()
                .email(request.email())
                .name(request.name())
                .role(Role.CANDIDATE)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .build();
        appUserRepository.save(user);

        Candidate candidate = Candidate.builder().user(user).build();
        Candidate saved = candidateRepository.save(candidate);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CandidateResponse> listCandidates(int page, int size, String search, String sortBy, String sortDir, AssessmentStatus status) {
        String sortField = mapSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        String searchParam = (search == null || search.isBlank()) ? null : search;

        Page<Candidate> candidates = candidateRepository.search(searchParam, status, pageable);
        return new PageResponse<>(
                candidates.getContent().stream().map(this::toResponse).toList(),
                candidates.getTotalElements(),
                candidates.getTotalPages(),
                candidates.getSize(),
                candidates.getNumber()
        );
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(UUID id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + id));
        return toResponse(candidate);
    }

    @Transactional
    public CandidateResponse updateCandidate(UUID id, CandidateRequest request) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + id));
        AppUser user = candidate.getUser();
        if (!user.getEmail().equals(request.email()) && appUserRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }
        user.setName(request.name());
        user.setEmail(request.email());
        appUserRepository.save(user);
        return toResponse(candidate);
    }

    @Transactional
    public void deleteCandidate(UUID id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + id));
        AppUser user = candidate.getUser();
        try {
            candidateRepository.delete(candidate);
            candidateRepository.flush();
            appUserRepository.delete(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Cannot delete candidate with existing assessments");
        }
    }

    private CandidateResponse toResponse(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getUser().getName(),
                candidate.getUser().getEmail(),
                candidate.getCreatedAt() != null ? candidate.getCreatedAt().toString() : null
        );
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy == null ? "name" : sortBy) {
            case "email" -> "user.email";
            case "createdAt" -> "createdAt";
            default -> "user.name";
        };
    }
}

package com.psybergate.dap.service;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.dto.CandidateRequest;
import com.psybergate.dap.dto.CandidateResponse;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.repository.AppUserRepository;
import com.psybergate.dap.repository.CandidateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return new CandidateResponse(saved.getId(), user.getName(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public PageResponse<CandidateResponse> listCandidates(int page, int size) {
        Page<Candidate> candidates = candidateRepository.findAll(PageRequest.of(page, size));
        return new PageResponse<>(
                candidates.getContent().stream()
                        .map(c -> new CandidateResponse(c.getId(), c.getUser().getName(), c.getUser().getEmail()))
                        .toList(),
                candidates.getTotalElements(),
                candidates.getTotalPages()
        );
    }
}

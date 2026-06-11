package com.psybergate.dap.service;

import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.QuestionBank;
import com.psybergate.dap.domain.ValidationException;
import com.psybergate.dap.dto.PageResponse;
import com.psybergate.dap.dto.QuestionBankRequest;
import com.psybergate.dap.dto.QuestionBankResponse;
import com.psybergate.dap.repository.QuestionBankRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class QuestionBankService {

    private final QuestionBankRepository questionBankRepository;

    public QuestionBankService(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    @Transactional
    public QuestionBankResponse create(QuestionBankRequest request) {
        if (questionBankRepository.findByName(request.name()).isPresent()) {
            throw new ConflictException("Question bank with name '" + request.name() + "' already exists");
        }
        QuestionBank bank = new QuestionBank(request.name());
        return toResponse(questionBankRepository.save(bank));
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionBankResponse> list(int page, int size) {
        Page<QuestionBank> result = questionBankRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getSize(),
                result.getNumber()
        );
    }

    @Transactional
    public QuestionBankResponse rename(UUID id, QuestionBankRequest request) {
        QuestionBank bank = questionBankRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question bank not found: " + id));
        if (!bank.getName().equals(request.name())) {
            questionBankRepository.findByName(request.name()).ifPresent(existing -> {
                throw new ConflictException("Question bank with name '" + request.name() + "' already exists");
            });
            bank.setName(request.name());
            questionBankRepository.save(bank);
        }
        return toResponse(bank);
    }

    @Transactional
    public void delete(UUID id) {
        QuestionBank bank = questionBankRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question bank not found: " + id));
        if (questionBankRepository.existsByQuestionsId(id)) {
            throw new ValidationException("Question bank '" + bank.getName() + "' is in use and cannot be deleted");
        }
        questionBankRepository.delete(bank);
    }

    private QuestionBankResponse toResponse(QuestionBank bank) {
        long count = questionBankRepository.countQuestionsByBankId(bank.getId());
        return new QuestionBankResponse(bank.getId(), bank.getName(), count);
    }
}

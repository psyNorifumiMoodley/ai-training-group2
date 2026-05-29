package com.psybergate.dap.dto;

public sealed interface QuestionResponse
        permits McqQuestionResponse, TextQuestionResponse, DocQuestionResponse, GroupQuestionResponse {
}
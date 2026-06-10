package com.psybergate.dap.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqQuestionResponse.class, name = "MCQ"),
        @JsonSubTypes.Type(value = McqPlusQuestionResponse.class, name = "MCQ_PLUS"),
        @JsonSubTypes.Type(value = TextQuestionResponse.class, name = "TEXT"),
        @JsonSubTypes.Type(value = DocQuestionResponse.class, name = "DOC"),
        @JsonSubTypes.Type(value = GroupQuestionResponse.class, name = "GROUP"),
        @JsonSubTypes.Type(value = CodingQuestionResponse.class, name = "CODING")
})
public sealed interface QuestionResponse
        permits McqQuestionResponse, McqPlusQuestionResponse, TextQuestionResponse, DocQuestionResponse, GroupQuestionResponse, CodingQuestionResponse {
}

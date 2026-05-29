package com.psybergate.dap.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqQuestionResponse.class, name = "MCQ"),
        @JsonSubTypes.Type(value = TextQuestionResponse.class, name = "TEXT"),
        @JsonSubTypes.Type(value = DocQuestionResponse.class, name = "DOC"),
        @JsonSubTypes.Type(value = GroupQuestionResponse.class, name = "GROUP")
})
public sealed interface QuestionResponse
        permits McqQuestionResponse, TextQuestionResponse, DocQuestionResponse, GroupQuestionResponse {
}
package com.psybergate.dap.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqQuestionRequest.class, name = "MCQ"),
        @JsonSubTypes.Type(value = DocQuestionRequest.class, name = "DOC"),
        @JsonSubTypes.Type(value = TextQuestionRequest.class, name = "TEXT"),
        @JsonSubTypes.Type(value = GroupQuestionRequest.class, name = "GROUP")
})
public sealed interface QuestionRequest permits McqQuestionRequest, DocQuestionRequest, TextQuestionRequest, GroupQuestionRequest {
}

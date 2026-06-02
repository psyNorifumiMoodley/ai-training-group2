package com.psybergate.dap.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = McqResponseRequest.class, name = "MCQ"),
        @JsonSubTypes.Type(value = TextResponseRequest.class, name = "TEXT"),
        @JsonSubTypes.Type(value = DocResponseRequest.class, name = "DOC"),
        @JsonSubTypes.Type(value = GroupResponseRequest.class, name = "GROUP")
})
public sealed interface ResponseRequest
        permits McqResponseRequest, TextResponseRequest, DocResponseRequest, GroupResponseRequest {
}

package com.psybergate.dap.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(McqResponseRequest.class),
        @JsonSubTypes.Type(TextResponseRequest.class),
        @JsonSubTypes.Type(DocResponseRequest.class),
        @JsonSubTypes.Type(GroupResponseRequest.class)
})
public sealed interface ResponseRequest
        permits McqResponseRequest, TextResponseRequest, DocResponseRequest, GroupResponseRequest {
}

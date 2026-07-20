package com.jobscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobRequest {

    @NotBlank(message = "type is required, e.g. SEND_EMAIL")
    private String type;

    // Free-form JSON string, e.g. {"to":"user@example.com","subject":"hi"}
    private String payload;

    // Optional override; defaults to 3 in the entity if not provided
    private Integer maxRetries;
}

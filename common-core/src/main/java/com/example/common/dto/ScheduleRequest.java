package com.example.common.dto;

import jakarta.validation.constraints.NotBlank;


public record ScheduleRequest(
        @NotBlank String name,
        @NotBlank String group,
        String cron,                 // either cron OR fixedDelay/fixedRate
        Long fixedDelayMs,
        Long fixedRateMs,
        @NotBlank String jobType,    // e.g., PRINT_MESSAGE
        String payload               // JSON/blob as string
) {}
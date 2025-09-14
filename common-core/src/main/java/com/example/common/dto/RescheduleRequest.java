// common/src/main/java/com/example/common/dto/RescheduleRequest.java
package com.example.common.dto;

public record RescheduleRequest(
        String newCron,
        Long newFixedDelayMs,
        Long newFixedRateMs
) {}
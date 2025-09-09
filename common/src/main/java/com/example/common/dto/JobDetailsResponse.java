// common/src/main/java/com/example/common/dto/JobDetailsResponse.java
package com.example.common.dto;

public record JobDetailsResponse(
        String id,
        String name,
        String group,
        String cron,
        String nextFireTime // ISO-8601 string or null
) {}

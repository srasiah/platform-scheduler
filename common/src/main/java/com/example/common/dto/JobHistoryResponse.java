// common/src/main/java/com/example/common/dto/JobHistoryResponse.java
package com.example.common.dto;

import java.time.OffsetDateTime;

public record JobHistoryResponse(
        String jobId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String outcome,
        String message
) {}

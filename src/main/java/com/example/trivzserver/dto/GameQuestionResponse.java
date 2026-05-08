package com.example.trivzserver.dto;

import java.time.LocalDateTime;

public record GameQuestionResponse(
        Long roomId,
        Long questionId,
        String prompt,
        String category,
        String difficulty,
        Integer timeLimitSeconds,
        Integer index,
        Integer total,
        LocalDateTime questionStartedAt
) {}
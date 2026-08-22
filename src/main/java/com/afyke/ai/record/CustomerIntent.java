package com.afyke.ai.record;

public record CustomerIntent(
        String intent,
        String summary,
        boolean needHumanConfirm
) {
}

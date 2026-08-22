package com.afyke.ai.record;

public record AfterSaleRequest(
        String orderNo,
        String issueType,
        String description,
        boolean needHumanConfirm
) {
}

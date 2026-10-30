package com.devisri.finance.insights;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InsightsController {

    private final InsightsService insights;

    public InsightsController(InsightsService insights) {
        this.insights = insights;
    }

    @GetMapping("/insights")
    InsightsResponse insights(@AuthenticationPrincipal String userId) {
        return insights.insights(userId);
    }
}

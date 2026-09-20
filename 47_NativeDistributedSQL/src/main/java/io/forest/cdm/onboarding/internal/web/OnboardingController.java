package io.forest.cdm.onboarding.internal.web;

import io.forest.cdm.onboarding.OnboardingCommand;
import io.forest.cdm.onboarding.OnboardingOutcome;
import io.forest.cdm.onboarding.OnboardingService;
import io.forest.cdm.onboarding.ResidencySnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP surface for the PoC scenarios. Keep this module free of business logic: it only adapts
 * HTTP to the in-process {@link OnboardingService}.
 */
@RestController
@RequestMapping("/api")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /** Pattern A: region-local transaction (UK only). */
    @PostMapping("/onboarding/local")
    public OnboardingOutcome onboardLocal(@RequestBody OnboardingCommand request) {
        return onboardingService.onboardUkLocal(request);
    }

    /** Pattern B: cross-region transaction (UK core domains + HK product holding). */
    @PostMapping("/onboarding/cross-region")
    public OnboardingOutcome onboardCrossRegion(@RequestBody OnboardingCommand request) {
        return onboardingService.onboardCrossRegion(request);
    }

    /**
     * Pattern B negative test. Returns the residency snapshot before and after the failed
     * attempt so the caller can see that {@code rolledBack} is {@code true}.
     */
    @PostMapping("/onboarding/cross-region/rollback-demo")
    public Map<String, Object> rollbackDemo(@RequestBody OnboardingCommand request) {
        ResidencySnapshot before = onboardingService.residencySnapshot();

        String outcome;
        try {
            onboardingService.onboardCrossRegionWithFailure(request);
            outcome = "UNEXPECTED: no exception was thrown";
        } catch (IllegalStateException ex) {
            outcome = ex.getMessage();
        }

        ResidencySnapshot after = onboardingService.residencySnapshot();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("simulatedFailure", outcome);
        body.put("rolledBack", before.equals(after));
        body.put("before", before);
        body.put("after", after);
        return body;
    }

    /** Data-residency proof: rows physically held per region, per table. */
    @GetMapping("/residency")
    public ResidencySnapshot residency() {
        return onboardingService.residencySnapshot();
    }
}

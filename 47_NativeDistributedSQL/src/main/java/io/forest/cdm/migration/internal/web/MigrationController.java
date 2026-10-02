package io.forest.cdm.migration.internal.web;

import io.forest.cdm.migration.LegacyCustomer;
import io.forest.cdm.migration.MigrationGap;
import io.forest.cdm.migration.MigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP surface for the migration proof of concept.
 *
 * <p>{@code /legacy-customers} stands in for the legacy system writing to its own store; the
 * remaining endpoints drive the migration itself.
 */
@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /** Simulates a write performed by the legacy system (MongoDB only). */
    @PostMapping("/legacy-customers")
    @ResponseStatus(HttpStatus.CREATED)
    public void recordLegacyWrite(@RequestBody LegacyCustomer customer) {
        migrationService.recordLegacyWrite(customer);
    }

    /** The strangler dual-write: CDM first, then mirrored into the legacy store. */
    @PostMapping("/customers")
    public Map<String, Object> dualWrite(@RequestBody LegacyCustomer customer) {
        UUID partyId = migrationService.dualWrite(customer);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partyId", partyId.toString());
        body.put("gap", migrationService.gap());
        return body;
    }

    @PostMapping("/backfill")
    public Map<String, Object> backfill() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("created", migrationService.backfill());
        body.put("gap", migrationService.gap());
        return body;
    }

    @PostMapping("/reconcile")
    public Map<String, Object> reconcile() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applied", migrationService.reconcile());
        body.put("gap", migrationService.gap());
        return body;
    }

    @GetMapping("/gap")
    public MigrationGap gap() {
        return migrationService.gap();
    }
}

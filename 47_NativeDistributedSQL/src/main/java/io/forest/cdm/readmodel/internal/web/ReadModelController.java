package io.forest.cdm.readmodel.internal.web;

import io.forest.cdm.readmodel.Customer360View;
import io.forest.cdm.readmodel.ReadModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Query side of Pattern C.
 *
 * <p>{@code /readmodel/project} drives one catch-up pass explicitly, which keeps the proof of
 * concept deterministic: a scenario can assert that the projection is <em>stale</em> immediately
 * after a write, and correct after the projector runs.
 */
@RestController
public class ReadModelController {

    private final ReadModelService readModelService;

    public ReadModelController(ReadModelService readModelService) {
        this.readModelService = readModelService;
    }

    @PostMapping("/api/readmodel/project")
    public Map<String, Object> project() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projected", readModelService.project());
        body.put("projectionRows", readModelService.projectionCount());
        body.put("lastRun", readModelService.lastRun());
        return body;
    }

    /** Served entirely from the projection. Returns 404 when the projection has not caught up. */
    @GetMapping("/api/customer-360/{partyId}")
    public ResponseEntity<Customer360View> customer360(@PathVariable UUID partyId) {
        return readModelService.read(partyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/readmodel/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectionRows", readModelService.projectionCount());
        body.put("lastRun", readModelService.lastRun());
        return body;
    }
}

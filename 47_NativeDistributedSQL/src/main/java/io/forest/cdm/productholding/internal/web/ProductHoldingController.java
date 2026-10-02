package io.forest.cdm.productholding.internal.web;

import io.forest.cdm.productholding.ProductHoldingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * PoC scenario A2: a region-local write that touches only the {@code hk} region.
 *
 * <p>Closing this gap matters because it is the counterpoint to A1: both are Pattern A
 * (region-local) transactions, but one is homed in {@code uk} and the other in {@code hk}.
 */
@RestController
@RequestMapping("/api/product-holdings")
public class ProductHoldingController {

    private final ProductHoldingService productHoldingService;

    public ProductHoldingController(ProductHoldingService productHoldingService) {
        this.productHoldingService = productHoldingService;
    }

    @PostMapping
    public ProductHoldingResult create(@RequestBody ProductHoldingRequest request) {
        UUID id = productHoldingService.createProductHolding(
                request.partyId(),
                request.productType(),
                request.accountNumber(),
                request.market());
        return new ProductHoldingResult(id, productHoldingService.homeRegion().sqlName(), "A_REGION_LOCAL");
    }
}

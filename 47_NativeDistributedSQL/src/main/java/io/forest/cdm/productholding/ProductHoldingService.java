package io.forest.cdm.productholding;

import io.forest.cdm.party.PartyService;
import io.forest.cdm.productholding.internal.ProductHoldingRepository;
import io.forest.cdm.shared.Region;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Public API of the {@code productholding} module (HK Market Domain).
 *
 * <p>Product holding data is mastered in the market region and is therefore written into the
 * {@code hk} region. Referential integrity against {@code party} is enforced <em>inside the same
 * transaction</em> as the insert, so a dangling reference can never be committed.
 */
@Service
public class ProductHoldingService {

    private static final Region HOME_REGION = Region.HK;

    private final ProductHoldingRepository repository;
    private final PartyService partyService;

    public ProductHoldingService(ProductHoldingRepository repository, PartyService partyService) {
        this.repository = repository;
        this.partyService = partyService;
    }

    /**
     * Opens a product holding against an existing party.
     *
     * @throws io.forest.cdm.party.PartyNotFoundException if the party does not exist, which aborts
     *         the transaction so no partial write survives
     */
    @Transactional
    public UUID createProductHolding(UUID partyId, String productType, String accountNumber, String market) {
        partyService.requirePartyExists(partyId);
        UUID id = UUID.randomUUID();
        repository.insert(id, partyId, productType, accountNumber, market);
        return id;
    }

    /** Read-model support: the product holdings that belong to a party. */
    public java.util.List<ProductHoldingRow> findForParty(java.util.UUID partyId) {
        return repository.findForParty(partyId);
    }

    /** The region this module's rows are pinned to, exposed so callers can assert residency. */
    public Region homeRegion() {
        return HOME_REGION;
    }

    /** Residency proof helper: how many product holding rows physically live in each region. */
    public Map<Region, Long> rowCountsByRegion() {
        return repository.countsByRegion();
    }
}

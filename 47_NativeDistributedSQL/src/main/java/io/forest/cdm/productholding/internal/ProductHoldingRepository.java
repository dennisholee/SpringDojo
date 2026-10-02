package io.forest.cdm.productholding.internal;

import io.forest.cdm.productholding.ProductHoldingRow;
import io.forest.cdm.shared.Region;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound port for product holding persistence, owned by the {@code productholding} module and
 * implemented by {@link JdbcProductHoldingRepository}.
 *
 * <p>This adapter pins rows to {@code hk}; combined with the UK-pinned {@code PartyRepository},
 * that difference is what makes an onboarding transaction span regions.
 */
public interface ProductHoldingRepository {

    void insert(UUID id, UUID partyId, String productType, String accountNumber, String market);

    List<ProductHoldingRow> findForParty(UUID partyId);

    Map<Region, Long> countsByRegion();
}

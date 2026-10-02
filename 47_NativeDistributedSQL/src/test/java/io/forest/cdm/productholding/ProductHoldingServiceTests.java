package io.forest.cdm.productholding;

import io.forest.cdm.party.PartyNotFoundException;
import io.forest.cdm.party.PartyService;
import io.forest.cdm.party.PartyType;
import io.forest.cdm.productholding.internal.ProductHoldingRepository;
import io.forest.cdm.shared.Region;
import io.forest.cdm.support.InMemoryPartyRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@code ProductHoldingService}, focused on the referential-integrity rule that the
 * end-to-end suite proves behaviourally: a holding for an unknown party must be rejected and must
 * leave nothing behind.
 */
class ProductHoldingServiceTests {

    private final InMemoryPartyRepository parties = new InMemoryPartyRepository();
    private final InMemoryProductHoldingRepository holdings = new InMemoryProductHoldingRepository();
    private final PartyService partyService = new PartyService(parties);
    private final ProductHoldingService service = new ProductHoldingService(holdings, partyService);

    @Test
    void opensAHoldingForAnExistingParty() {
        UUID partyId = partyService.createParty(PartyType.ORGANIZATION, "Holder Ltd", Region.UK);

        UUID holdingId = service.createProductHolding(
                partyId, "SAVINGS_ACCOUNT", "HK-1", "HK_INSURANCE");

        assertThat(holdingId).isNotNull();
        assertThat(holdings.accountNumbers).containsExactly("HK-1");
    }

    @Test
    void refusesAHoldingForAnUnknownPartyAndWritesNothing() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.createProductHolding(
                unknown, "SAVINGS_ACCOUNT", "HK-2", "HK_INSURANCE"))
                .isInstanceOf(PartyNotFoundException.class)
                .hasMessageContaining(unknown.toString());

        assertThat(holdings.accountNumbers)
                .as("a rejected write must leave nothing behind")
                .isEmpty();
    }

    @Test
    void reportsItsHomeRegion() {
        assertThat(service.homeRegion()).isEqualTo(Region.HK);
    }

    @Test
    void exposesHoldingsForTheReadModel() {
        UUID partyId = partyService.createParty(PartyType.ORGANIZATION, "Read Model Ltd", Region.UK);
        service.createProductHolding(partyId, "SAVINGS_ACCOUNT", "HK-3", "HK_INSURANCE");

        assertThat(service.findForParty(partyId))
                .singleElement()
                .satisfies(row -> assertThat(row.accountNumber()).isEqualTo("HK-3"));
    }

    /** Hand-written fake adapter for the {@code productholding} port. */
    private static final class InMemoryProductHoldingRepository implements ProductHoldingRepository {

        private final List<String> accountNumbers = new ArrayList<>();
        private final List<UUID> owners = new ArrayList<>();

        @Override
        public void insert(UUID id, UUID partyId, String productType, String accountNumber, String market) {
            owners.add(partyId);
            accountNumbers.add(accountNumber);
        }

        @Override
        public List<ProductHoldingRow> findForParty(UUID partyId) {
            List<ProductHoldingRow> rows = new ArrayList<>();
            for (int i = 0; i < owners.size(); i++) {
                if (owners.get(i).equals(partyId)) {
                    rows.add(new ProductHoldingRow(accountNumbers.get(i), "HK_INSURANCE"));
                }
            }
            return rows;
        }

        @Override
        public Map<Region, Long> countsByRegion() {
            Map<Region, Long> counts = new EnumMap<>(Region.class);
            if (!accountNumbers.isEmpty()) {
                counts.put(Region.HK, (long) accountNumbers.size());
            }
            return counts;
        }
    }
}

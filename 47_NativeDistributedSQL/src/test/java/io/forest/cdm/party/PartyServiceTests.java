package io.forest.cdm.party;

import io.forest.cdm.shared.Region;
import io.forest.cdm.support.InMemoryPartyRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@code party} application service.
 *
 * <p>They drive the service through a <b>hand-written fake adapter</b> that implements the
 * {@link PartyRepository} port. No mocking framework and no Spring context: this is exactly the
 * payoff of extracting the port, and it covers the failure branches the end-to-end suite cannot
 * reach deterministically.
 */
class PartyServiceTests {

    private final InMemoryPartyRepository repository = new InMemoryPartyRepository();
    private final PartyService service = new PartyService(repository);

    @Test
    void createsAPartyAndDelegatesTheRegionToTheAdapter() {
        UUID id = service.createParty(PartyType.ORGANIZATION, "Acme Ltd", Region.UK);

        assertThat(id).isNotNull();
        assertThat(repository.storedNames().values()).containsExactly("Acme Ltd");
        assertThat(repository.storedRegions().values()).containsExactly(Region.UK);
    }

    @Test
    void requirePartyExistsAcceptsAKnownParty() {
        UUID id = service.createParty(PartyType.INDIVIDUAL, "Ada Lovelace", Region.UK);

        service.requirePartyExists(id);
    }

    @Test
    void requirePartyExistsRejectsAnUnknownParty() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.requirePartyExists(unknown))
                .isInstanceOf(PartyNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    @Test
    void countsPartiesByLegalNamePrefix() {
        service.createParty(PartyType.ORGANIZATION, "MIG-A", Region.UK);
        service.createParty(PartyType.ORGANIZATION, "MIG-B", Region.UK);
        service.createParty(PartyType.ORGANIZATION, "OTHER", Region.UK);

        assertThat(service.countByLegalNamePrefix("MIG-")).isEqualTo(2);
    }

    @Test
    void exposesPartyRowsForTheReadModel() {
        service.createParty(PartyType.ORGANIZATION, "Projected Ltd", Region.UK);

        assertThat(service.listParties(10))
                .singleElement()
                .satisfies(row -> assertThat(row.legalName()).isEqualTo("Projected Ltd"));
    }
}

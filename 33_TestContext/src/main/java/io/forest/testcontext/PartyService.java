package io.forest.testcontext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartyService {

    @NonNull
    PartyRepository partyRepository;

    @NonNull
    AddressAPIClient addressAPIClient;

    public Party findById(UUID id) {
        log.info("Find by ID [id={}]", id);
        return Optional.ofNullable(id)
            .map(partyRepository::findById)
            .map(enrichWithAddress)
            .orElse(null);
    }

    public void onboardParty(UUID partyId, String firatName, String lastName) {
        this.partyRepository.save(
            new Party(partyId, "John", "Smith", null));
    }

    public void updateName(UUID id, String firstName, String lastName) {

        Optional.ofNullable(this.partyRepository.findById(id))
            .map(it -> it.setFirstName(firstName).setLastName(lastName))
            .ifPresentOrElse(
                this.partyRepository::save,
                () -> {
                    throw new RuntimeException("ID not found [id=%s]".formatted(id));
                }
            );

    }

    Function<Party, Party> enrichWithAddress = party ->
        Optional.ofNullable(this.addressAPIClient.findByPartyId(party.getPartyId()))
            .map(party::setAddress)
            .orElse(party);

}

package io.forest.testcontext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartyService {

    @NonNull
    PartyRepository partyRepository;

    public Party findById(UUID id) {
        log.info("Find by ID [id={}]", id);
        return Optional.ofNullable(id)
            .map(partyRepository::findById)
            .orElse(null);
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
}

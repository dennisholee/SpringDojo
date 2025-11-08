package io.forest.testcontext;

import org.springframework.stereotype.Component;

import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

@Component
public class PartyRepository {

    final Map<UUID, Party> parties;

    public PartyRepository() {
        parties = new Hashtable<>();
    }

    public Party findById(UUID id) {
        return this.parties.get(id);
    }

    public void save(Party party) {
        this.parties.put(party.getPartyId(), party);
    }
}

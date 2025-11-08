package io.forest.testcontext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class Party {

    UUID partyId;

    String firstName;

    String lastName;
}

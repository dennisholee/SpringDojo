package io.forest.testcontext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@AllArgsConstructor
@Accessors(chain = true)
@ToString
public class Party {

    UUID partyId;

    String firstName;

    String lastName;

    Address address;
}

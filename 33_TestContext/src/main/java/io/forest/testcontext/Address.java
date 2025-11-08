package io.forest.testcontext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Data
@Accessors(chain = true)
public class Address {

    String addressLine1;

    String addressLine2;

}

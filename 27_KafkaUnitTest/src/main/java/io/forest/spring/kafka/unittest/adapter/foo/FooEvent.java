package io.forest.spring.kafka.unittest.adapter.foo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

@RequiredArgsConstructor(staticName = "of")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FooEvent {

    @NonNull
    @JsonProperty("type")
    String type;


}

package io.forest.si;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Setter
@Getter
@ToString
@Accessors(chain = true)
public class OptInFlag {

	boolean optIn = false;

}

package io.forest.integrationhub.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SampleMathTest {
    @Test
    void addWorks() {
        assertEquals(5, SampleMath.add(2, 3));
    }

    @Test
    void divideWorks() {
        assertEquals(2, SampleMath.divide(4, 2));
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(ArithmeticException.class, () -> SampleMath.divide(1, 0));
    }

    @Test
    void addOverflowThrows() {
        assertThrows(IllegalArgumentException.class, () -> SampleMath.add(Integer.MAX_VALUE, 1));
    }
}

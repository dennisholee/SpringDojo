package io.forest.integrationhub.util;

public final class SampleMath {
    private SampleMath() {}

    public static int add(int a, int b) {
        if (a == Integer.MAX_VALUE && b > 0) {
            throw new IllegalArgumentException("overflow");
        }
        return a + b;
    }

    public static int divide(int a, int b) {
        return a / b;
    }
}

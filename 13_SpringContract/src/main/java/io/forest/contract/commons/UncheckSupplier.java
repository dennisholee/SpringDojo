package io.forest.contract.commons;

import java.util.function.Supplier;

@FunctionalInterface
public interface UncheckSupplier<T, E extends Exception> {

    T get() throws E;

    static <T> T unchecked(UncheckSupplier<? extends T, ?> supplier) {
        try {
            return supplier.get();
        } catch(Exception e) {
            throw new RuntimeException();
        }
    }
}

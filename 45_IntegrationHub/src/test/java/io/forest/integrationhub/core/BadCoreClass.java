package io.forest.integrationhub.core;

import io.forest.integrationhub.adapters.web.SamplePortImpl;

/**
 * Seeded failing example: core class illegally depends on adapter implementation.
 * This should be caught by ArchUnit rules (core must not depend on adapters).
 */
public class BadCoreClass {
    private final SamplePortImpl impl = new SamplePortImpl();

    public void process() {
        impl.call();
    }
}

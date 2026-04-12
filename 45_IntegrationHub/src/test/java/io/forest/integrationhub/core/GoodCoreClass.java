package io.forest.integrationhub.core;

import io.forest.integrationhub.ports.SamplePort;

public class GoodCoreClass {
    private final SamplePort port;

    public GoodCoreClass(SamplePort port) {
        this.port = port;
    }

    public void process() {
        port.call();
    }
}

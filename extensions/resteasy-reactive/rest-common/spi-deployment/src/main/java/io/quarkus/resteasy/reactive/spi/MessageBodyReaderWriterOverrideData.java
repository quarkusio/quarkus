package io.quarkus.resteasy.reactive.spi;

public final class MessageBodyReaderWriterOverrideData {

    private final int priority;
    private final boolean builtIn;

    public MessageBodyReaderWriterOverrideData(int priority, boolean builtIn) {
        this.priority = priority;
        this.builtIn = builtIn;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }
}

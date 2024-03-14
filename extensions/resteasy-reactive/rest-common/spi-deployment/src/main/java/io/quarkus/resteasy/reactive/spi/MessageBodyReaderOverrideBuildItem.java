package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class MessageBodyReaderOverrideBuildItem extends MultiBuildItem {

    private final String className;
    private final MessageBodyReaderWriterOverrideData overrideData;

    public MessageBodyReaderOverrideBuildItem(String className, int priority, boolean builtIn) {
        this.className = className;
        this.overrideData = new MessageBodyReaderWriterOverrideData(priority, builtIn);

    }

    public String getClassName() {
        return className;
    }

    public MessageBodyReaderWriterOverrideData getOverrideData() {
        return overrideData;
    }
}

package io.quarkus.registry.catalog;

import java.util.Collection;

public interface PlatformStack {

    String getPlatformKey();

    String getPlatformName();

    Collection<PlatformStream> getStreams();

    PlatformStream getStream(String streamId);
}

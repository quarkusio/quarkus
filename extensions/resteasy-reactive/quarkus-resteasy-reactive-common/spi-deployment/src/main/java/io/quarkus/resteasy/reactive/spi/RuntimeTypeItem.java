package io.quarkus.resteasy.reactive.spi;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.ws.rs.RuntimeType;

public interface RuntimeTypeItem {

    /**
     * Returns the runtime type for this build item. If the value is null
     * then it applies to both server and client.
     */
    RuntimeType getRuntimeType();

    static <T extends RuntimeTypeItem> Collection<T> filter(Collection<T> items, RuntimeType current) {
        return items.stream().filter(s -> s.getRuntimeType() == null || s.getRuntimeType() == current)
                .collect(Collectors.toList());
    }
}

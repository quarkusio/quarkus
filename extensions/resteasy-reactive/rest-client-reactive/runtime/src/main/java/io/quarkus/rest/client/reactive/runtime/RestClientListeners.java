package io.quarkus.rest.client.reactive.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.microprofile.rest.client.spi.RestClientListener;

public class RestClientListeners {

    private RestClientListeners() {
    }

    static Collection<RestClientListener> get() {
        List<RestClientListener> result = new ArrayList<>();
        ServiceLoader<RestClientListener> listeners = ServiceLoader.load(RestClientListener.class,
                RestClientListeners.class.getClassLoader());
        for (RestClientListener listener : listeners) {
            result.add(listener);
        }
        return result;
    }
}

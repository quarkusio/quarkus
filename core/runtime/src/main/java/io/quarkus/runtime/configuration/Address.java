package io.quarkus.runtime.configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public final class Address {
    private final String address;

    public Address(final String address) {
        Objects.requireNonNull(address);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public URI getAsUri() throws URISyntaxException {
        Optional<Services.Service> service = Services.get(address);
        if (service.isPresent()) {
            return service.get().toUri();
        }
        return new URI(address);
    }

    public URL getAsUrl() throws MalformedURLException {
        Optional<Services.Service> service = Services.get(address);
        if (service.isPresent()) {
            return service.get().toUrl();
        }
        return URI.create(address).toURL();
    }

    @Override
    public String toString() {
        return address;
    }
}

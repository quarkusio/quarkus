package io.quarkus.it.cache.infinispan;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class ClientRequestService {
    String data;

    public String data() {
        return data;
    }

    public ClientRequestService setData(String data) {
        this.data = data;
        return this;
    }
}

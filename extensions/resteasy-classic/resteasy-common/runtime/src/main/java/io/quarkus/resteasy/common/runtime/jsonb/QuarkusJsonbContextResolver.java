package io.quarkus.resteasy.common.runtime.jsonb;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
@Priority(Priorities.USER + 10) // give it a priority that ensures that user supplied ContextResolver classes override
                                                             // this one
public class QuarkusJsonbContextResolver implements ContextResolver<Jsonb> {

    private final Jsonb jsonb;

    public QuarkusJsonbContextResolver(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }
}

package io.quarkus.resteasy.common.runtime.jsonb;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
@Priority(Priorities.USER + 10) // give it a priority that ensures that user supplied ContextResolver classes override this one
public class QuarkusJsonbContextResolver implements ContextResolver<Jsonb> {

    @Inject
    Jsonb jsonb;

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }
}

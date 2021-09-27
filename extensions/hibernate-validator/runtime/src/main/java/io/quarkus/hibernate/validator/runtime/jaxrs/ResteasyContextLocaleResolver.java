package io.quarkus.hibernate.validator.runtime.jaxrs;

import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.core.ResteasyContext;

import io.quarkus.arc.DefaultBean;

@Singleton
@DefaultBean
public class ResteasyContextLocaleResolver extends AbstractLocaleResolver {

    @Override
    protected HttpHeaders getHeaders() {
        return ResteasyContext.getContextData(HttpHeaders.class);
    }
}

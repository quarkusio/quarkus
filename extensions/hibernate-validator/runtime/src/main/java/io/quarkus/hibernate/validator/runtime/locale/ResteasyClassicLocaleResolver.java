package io.quarkus.hibernate.validator.runtime.locale;

import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.core.ResteasyContext;

@Singleton
public class ResteasyClassicLocaleResolver extends AbstractLocaleResolver {

    @Override
    protected Map<String, List<String>> getHeaders() {
        HttpHeaders httpHeaders = ResteasyContext.getContextData(HttpHeaders.class);
        if (httpHeaders != null) {
            return httpHeaders.getRequestHeaders();
        } else {
            return null;
        }
    }
}

package io.quarkus.resteasy.reactive.jackson.runtime.security;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class SecurityJacksonAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public Object findFilterId(Annotated a) {
        return SecurityPropertyFilter.FILTER_ID;
    }
}

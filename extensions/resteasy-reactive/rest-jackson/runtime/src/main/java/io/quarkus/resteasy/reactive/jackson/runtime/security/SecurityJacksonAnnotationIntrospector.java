package io.quarkus.resteasy.reactive.jackson.runtime.security;

import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class SecurityJacksonAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public Object findFilterId(MapperConfig<?> config, Annotated a) {
        return SecurityPropertyFilter.FILTER_ID;
    }
}

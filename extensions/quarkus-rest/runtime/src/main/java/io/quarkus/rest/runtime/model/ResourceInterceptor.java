package io.quarkus.rest.runtime.model;

import java.util.Set;

public interface ResourceInterceptor {

    Set<String> getNameBindingNames();
}

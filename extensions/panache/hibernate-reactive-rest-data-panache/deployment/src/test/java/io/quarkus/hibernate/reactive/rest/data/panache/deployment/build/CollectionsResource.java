package io.quarkus.hibernate.reactive.rest.data.panache.deployment.build;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheEntityResource;
import io.quarkus.resteasy.reactive.server.EndpointDisabled;

@EndpointDisabled(name = "collections.endpoint", stringValue = "disable")
public interface CollectionsResource extends PanacheEntityResource<Collection, String> {
}

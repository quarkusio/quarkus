package io.quarkus.hibernate.orm.rest.data.panache.deployment.build;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;

@IfBuildProperty(name = "collections.enabled", stringValue = "true")
public interface CollectionsResource extends PanacheEntityResource<Collection, String> {
}

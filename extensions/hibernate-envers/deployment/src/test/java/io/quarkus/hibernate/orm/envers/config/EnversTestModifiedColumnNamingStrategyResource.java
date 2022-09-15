
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.envers.boot.internal.ImprovedModifiedColumnNamingStrategy;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-modified-column-naming-strategy")
@ApplicationScoped
public class EnversTestModifiedColumnNamingStrategyResource extends AbstractEnversResource {
    @GET
    public String getModifiedNamingStrategy() {
        Class<?> expectedClass = ImprovedModifiedColumnNamingStrategy.class;
        Class<?> actualClass = getGlobalConfiguration().getModifiedColumnNamingStrategy().getClass();
        if (actualClass.equals(expectedClass)) {
            return "OK";
        }
        return "Expected modified_column_naming_strategy to be " + expectedClass.getName() + " but was: "
                + actualClass.getName();
    }
}

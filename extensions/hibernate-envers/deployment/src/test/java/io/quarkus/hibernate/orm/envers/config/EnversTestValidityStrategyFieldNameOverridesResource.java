
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-validity-strategy-field-name-overrides")
@ApplicationScoped
public class EnversTestValidityStrategyFieldNameOverridesResource extends AbstractEnversResource {
    @GET
    public String getValidityStrategyFieldNameOverrides() {
        boolean isRevEndTimestampIncluded = getAuditEntitiesConfiguration().isRevisionEndTimestampEnabled();
        if (!isRevEndTimestampIncluded) {
            return "Expected audit_strategy_validity_store_revend_timestamp to be true but was false";
        }

        String revEndFieldName = getAuditEntitiesConfiguration().getRevisionEndFieldName();
        if (!revEndFieldName.equals("REV_END")) {
            return "Expected audit_strategy_validity_end_rev_field_name to be REV_END but was: " + revEndFieldName;
        }

        String revEndTimestampFieldName = getAuditEntitiesConfiguration().getRevisionEndTimestampFieldName();
        if (!revEndTimestampFieldName.equals("REV_END_TSTMP")) {
            return "Expected audit_strategy_validity_revend_timestamp_field_name to be REV_END_TSTMP but was: "
                    + revEndTimestampFieldName;
        }

        return "OK";
    }
}

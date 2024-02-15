package io.quarkus.hibernate.orm.runtime;

import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class HibernateOrmDisabledRecorder {

    public void checkNoExplicitActiveTrue(HibernateOrmRuntimeConfig runtimeConfig) {
        for (var entry : runtimeConfig.persistenceUnits().entrySet()) {
            var config = entry.getValue();
            if (config.active().isPresent() && config.active().get()) {
                var puName = entry.getKey();
                String enabledPropertyKey = HibernateOrmRuntimeConfig.extensionPropertyKey("enabled");
                String activePropertyKey = HibernateOrmRuntimeConfig.puPropertyKey(puName, "active");
                throw new ConfigurationException(
                        "Hibernate ORM activated explicitly for persistence unit '" + puName
                                + "', but the Hibernate ORM extension was disabled at build time."
                                + " If you want Hibernate ORM to be active for this persistence unit, you must set '"
                                + enabledPropertyKey
                                + "' to 'true' at build time."
                                + " If you don't want Hibernate ORM to be active for this persistence unit, you must leave '"
                                + activePropertyKey
                                + "' unset or set it to 'false'.",
                        Set.of(enabledPropertyKey, activePropertyKey));
            }
        }
    }

}

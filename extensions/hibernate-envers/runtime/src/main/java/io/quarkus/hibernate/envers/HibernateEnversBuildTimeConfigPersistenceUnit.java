package io.quarkus.hibernate.envers;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class HibernateEnversBuildTimeConfigPersistenceUnit {

    /**
     * Whether Hibernate Envers should be active for this persistence unit at runtime.
     *
     * If Hibernate Envers is not active, the audit entities will *still* be added to the Hibernate ORM metamodel
     * and to the database schema that Hibernate ORM generates:
     * you would need to disable Hibernate Envers at build time (i.e. set `quarkus.hibernate-envers.enabled` to `false`)
     * in order to avoid that.
     * However, when Hibernate Envers is not active, it will not process entity change events
     * nor create new versions of entities.
     * and accessing the AuditReader through AuditReaderFactory will not be possible.
     *
     * Note that if Hibernate Envers is disabled (i.e. `quarkus.hibernate-envers.enabled` is set to `false`),
     * it won't be active for any persistence unit, and setting this property to `true` will fail.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValueDocumentation = "`true` if Hibernate ORM is enabled; `false` otherwise")
    public Optional<Boolean> active = Optional.empty();

}

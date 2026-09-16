package io.quarkus.hibernate.orm.runtime.integration;

/**
 * @deprecated Use
 *             {@link io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationRuntimeInitListener}
 *             instead.
 */
@Deprecated(since = "4.0", forRemoval = true)
public interface HibernateOrmIntegrationRuntimeInitListener
        extends io.quarkus.hibernate.orm.runtime.spi.HibernateOrmIntegrationRuntimeInitListener {

}

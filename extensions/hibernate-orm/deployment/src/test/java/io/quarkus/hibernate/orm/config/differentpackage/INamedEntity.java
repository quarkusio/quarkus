package io.quarkus.hibernate.orm.config.differentpackage;

import io.quarkus.hibernate.orm.config.packages.ConfigEntityPUAssigmentUsingInterfaceTest;

/**
 * This must be located in a different package than the entity that implements it.
 * In particular, it must not be located in a subpackage of that entity's package.
 * Otherwise {@link ConfigEntityPUAssigmentUsingInterfaceTest} would not reproduce the problem correctly.
 */
public interface INamedEntity {
    String getName();
}

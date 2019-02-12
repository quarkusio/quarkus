package org.jboss.shamrock.jpa;

import org.jboss.builder.item.SimpleBuildItem;

/**
 * Purely marker build item so that you can register enhancers after Hibernate
 * registers its enhancers, which would make your enhancers run before the
 * Hibernate enhancers
 */
public final class HibernateEnhancersRegisteredBuildItem extends SimpleBuildItem {

}

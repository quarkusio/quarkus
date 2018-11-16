package org.jboss.shamrock.jdbc.postresql.deployment;

import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * Registers the {@code org.postgresql.Driver} so that it can be loaded
 * by reflection, as commonly expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class PostgreSQLJDBCReflections {

	@BuildStep
	void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
		//Not strictly necessary when using Agroal, as it also registers
		//any JDBC driver being configured explicitly through its configuration.
		//We register it for the sake of other users.
		final String driverName = "org.postgresql.Driver";
		reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driverName));
	}

}

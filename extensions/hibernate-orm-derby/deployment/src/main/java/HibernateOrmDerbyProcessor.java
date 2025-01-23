import java.util.Set;

import org.hibernate.community.dialect.CommunityDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;

public final class HibernateOrmDerbyProcessor {
    @BuildStep
    public DatabaseKindDialectBuildItem registerHibernateOrmMetadataForDerbyDialect() {
        return DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.DERBY, "Apache Derby",
                Set.of("org.hibernate.community.dialect.DerbyDialect"));
    }

    @BuildStep
    public ServiceProviderBuildItem registerCommunityDialectResolver() {
        return new ServiceProviderBuildItem(DialectResolver.class.getName(), CommunityDialectResolver.class.getName());
    }
}

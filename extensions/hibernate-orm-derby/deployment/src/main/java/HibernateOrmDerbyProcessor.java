import java.util.Set;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;

public final class HibernateOrmDerbyProcessor {
    @BuildStep
    void registerHibernateOrmMetadataForDerbyDialect(BuildProducer<DatabaseKindDialectBuildItem> producer) {
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.DERBY, "Apache Derby",
                Set.of("org.hibernate.community.dialect.DerbyDialect")));
    }
}

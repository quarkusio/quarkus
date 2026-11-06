package io.quarkus.hibernate.orm.deployment.cdi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.jandex.DotName.createSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItemTestHelper;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.recording.BytecodeRecorderImpl.ReturnedProxy;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.ProcessorTestStubs;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;

class HibernateOrmCdiProcessorMultitenancyTest {

    private static final HibernateOrmCdiProcessor processor = new HibernateOrmCdiProcessor();

    @Test
    void registersNothingWhenMultitenancyIsDisabled() {
        var output = registerMultitenancyBeans(null, MultiTenancyStrategy.NONE);

        assertThat(output.syntheticBeans).isEmpty();
        assertThat(output.unremovableBeans).isEmpty();
    }

    @Test
    void protectsTenantResolversForDiscriminatorMultitenancy() {
        var output = registerMultitenancyBeans(null, MultiTenancyStrategy.DISCRIMINATOR);

        assertThat(output.syntheticBeans).isEmpty();
        assertThat(output.unremovableBeans)
                .flatExtracting(UnremovableBeanBuildItem::getClassNames)
                .containsExactlyInAnyOrder(ClassNames.TENANT_CONNECTION_RESOLVER, ClassNames.TENANT_RESOLVER);
    }

    @ParameterizedTest
    @EnumSource(value = MultiTenancyStrategy.class, names = { "DATABASE", "SCHEMA" })
    void registersConnectionResolverForDatabaseAndSchemaMultitenancy(MultiTenancyStrategy strategy) {
        var output = registerMultitenancyBeans(new TestHibernateOrmRecorder(), strategy);

        assertThat(output.syntheticBeans).hasSize(1);
        var bean = SyntheticBeanBuildItemTestHelper.metadata(output.syntheticBeans.getFirst());

        assertThat(bean.implementationClass()).isEqualTo(createSimple(DataSourceTenantConnectionResolver.class));

        assertThat(bean.types()).extracting(type -> type.name())
                .containsExactlyInAnyOrder(createSimple(DataSourceTenantConnectionResolver.class),
                        createSimple(TenantConnectionResolver.class));

        assertThat(bean.runtimeInit()).isTrue();

        var namedQualifiers = qualifiersWithName(bean, DotNames.NAMED);
        assertThat(namedQualifiers).hasSize(1);
        assertThat(namedQualifiers.getFirst().value().asString()).isEqualTo("inventory");

        var persistenceUnitQualifiers = qualifiersWithName(bean, ClassNames.QUARKUS_PERSISTENCE_UNIT);
        assertThat(persistenceUnitQualifiers).hasSize(1);
        assertThat(persistenceUnitQualifiers.getFirst().value().asString()).isEqualTo("inventory");
    }

    private static @NonNull List<AnnotationInstance> qualifiersWithName(
            SyntheticBeanBuildItemTestHelper.SyntheticBeanMetadata bean, DotName named) {
        return bean.qualifiers().stream()
                .filter(qualifier -> qualifier.name().equals(named)).toList();
    }

    private static MultitenancyBeanRegistration registerMultitenancyBeans(
            HibernateOrmRecorder recorder,
            MultiTenancyStrategy strategy) {

        var syntheticBeans = new ArrayList<SyntheticBeanBuildItem>();
        var unremovableBeans = new ArrayList<UnremovableBeanBuildItem>();

        processor.multitenancy(recorder,
                List.of(ProcessorTestStubs.persistenceUnitDescriptor("inventory", strategy)),
                syntheticBeans::add, unremovableBeans::add);

        return new MultitenancyBeanRegistration(syntheticBeans, unremovableBeans);
    }

    private static final class TestHibernateOrmRecorder extends HibernateOrmRecorder {
        private TestHibernateOrmRecorder() {
            super(null, null);
        }

        @Override
        public Supplier<DataSourceTenantConnectionResolver> dataSourceTenantConnectionResolver(String persistenceUnitName,
                Optional<String> dataSourceName, MultiTenancyStrategy strategy) {
            return new RecorderSupplier();
        }
    }

    private static final class RecorderSupplier implements Supplier<DataSourceTenantConnectionResolver>, ReturnedProxy {
        @Override
        public DataSourceTenantConnectionResolver get() {
            return null;
        }

        @Override
        public String __returned$proxy$key() {
            return "test";
        }

        @Override
        public boolean __static$$init() {
            return false;
        }
    }

    private record MultitenancyBeanRegistration(List<SyntheticBeanBuildItem> syntheticBeans,
            List<UnremovableBeanBuildItem> unremovableBeans) {
    }
}

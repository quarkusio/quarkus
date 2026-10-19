package io.quarkus.hibernate.orm.deployment.cdi;

import static io.quarkus.hibernate.orm.deployment.ProcessorTestStubs.persistenceUnitDescriptor;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.jandex.DotName.createSimple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.transaction.TransactionManager;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.ProcessorTestStubs;
import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.hibernate.orm.runtime.RequestScopedStatelessSessionHolder;
import io.quarkus.hibernate.orm.runtime.TransactionSessions;
import io.quarkus.hibernate.orm.runtime.cdi.QuarkusArcBeanContainer;

/**
 * Documents the CDI processor: it registers Hibernate qualifiers and scopes, keeps dynamically used model beans,
 * and adds the session support beans required by blocking persistence units when transactions are available.
 */
class HibernateOrmCdiProcessorTest {

    private final HibernateOrmCdiProcessor processor = new HibernateOrmCdiProcessor();

    @Test
    void registersMandatoryBeanAnnotations() {
        var beans = new ArrayList<AdditionalBeanBuildItem>();
        var beanDefiningAnnotations = new ArrayList<BeanDefiningAnnotationBuildItem>();

        processor.registerAnnotations(beans::add, beanDefiningAnnotations::add);

        assertThat(beans).hasSize(1);
        assertThat(beans.getFirst().getBeanClasses()).containsExactlyInAnyOrder(
                PersistenceUnit.class.getName(),
                PersistenceUnitExtension.class.getName(),
                ClassNames.JSON_FORMAT.toString(),
                ClassNames.XML_FORMAT.toString());

        assertThat(beanDefiningAnnotations).hasSize(1);
        var annotation = beanDefiningAnnotations.getFirst();
        assertThat(annotation.getName()).isEqualTo(ClassNames.PERSISTENCE_UNIT_EXTENSION);
        assertThat(annotation.getDefaultScope()).isEqualTo(DotNames.APPLICATION_SCOPED);
        assertThat(annotation.isRemovable()).isFalse();
    }

    @Test
    void registersNoSupportingBeansWhenThereIsNoPersistenceUnit() {
        List<PersistenceUnitDescriptorBuildItem> persistenceUnits = emptyList();
        var output = registerSupportingBeans(
                new Capabilities(emptySet()),
                persistenceUnits,
                PotentialBean.class);

        assertThat(output.additionalBeans).isEmpty();
        assertThat(output.automaticBeans).isEmpty();
        assertThat(output.unremovableBeans).isEmpty();
    }

    @Test
    void avoidRegisteringTransactionalSupportWithoutTransactions() {
        // Empty capabilities => No Transaction Support
        var output = registerSupportingBeans(
                new Capabilities(emptySet()),
                List.of(persistenceUnitDescriptor()),
                PotentialBean.class);

        assertThat(output.additionalBeans).singleElement().satisfies(item -> {
            assertThat(item.getBeanClasses()).containsExactly(QuarkusArcBeanContainer.class.getName());
            assertThat(item.getBeanClasses()).doesNotContain(
                    TransactionManager.class.getName(),
                    TransactionSessions.class.getName(),
                    RequestScopedSessionHolder.class.getName(),
                    RequestScopedStatelessSessionHolder.class.getName());
            assertThat(item.isRemovable()).isFalse();
        });

    }

    @Test
    void addsTransactionBackedSessionSupportWhenTransactionsAreAvailable() {
        var output = registerSupportingBeans(
                new Capabilities(Set.of(Capability.TRANSACTIONS)),
                List.of(persistenceUnitDescriptor()),
                PotentialBean.class);

        assertThat(output.additionalBeans).singleElement().satisfies(item -> assertThat(item.getBeanClasses())
                .containsExactlyInAnyOrder(
                        QuarkusArcBeanContainer.class.getName(),
                        TransactionManager.class.getName(),
                        TransactionSessions.class.getName(),
                        RequestScopedSessionHolder.class.getName(),
                        RequestScopedStatelessSessionHolder.class.getName()));

    }

    @Test
    void protectsPotentialBeansAndAttributeConvertersFromRemoval() {
        var output = registerSupportingBeans(
                new Capabilities(emptySet()),
                List.of(persistenceUnitDescriptor()),
                PotentialBean.class);

        assertDoNotRemoveBean(output.unremovableBeans, PotentialBean.class);
        assertDoNotRemoveBean(output.unremovableBeans, AttributeConverter.class); // Implicitly protected from removal

        assertBeanCanBeRemoved(output.unremovableBeans, UnrelatedBean.class);
    }

    @Test
    void onlyPotentialBeansAreEligibleForAutomaticDependentScope() {
        var output = registerSupportingBeans(
                new Capabilities(emptySet()),
                List.of(persistenceUnitDescriptor()),
                PotentialBean.class);

        assertThat(output.automaticBeans).singleElement().satisfies(item -> {
            assertThat(item.getDefaultScope()).isEqualTo(BuiltinScope.DEPENDENT.getName());
            assertThat(item.isContainerServicesRequired()).isTrue();
            assertThat(isEligibleForAutomaticScope(item, PotentialBean.class)).isTrue();
            assertThat(isEligibleForAutomaticScope(item, UnrelatedBean.class)).isFalse();
        });
    }

    private BeanRegistration registerSupportingBeans(
            Capabilities capabilities,
            List<PersistenceUnitDescriptorBuildItem> descriptors, Class<?> potentialBeans) {
        var additionalBeans = new ArrayList<AdditionalBeanBuildItem>();
        var automaticBeans = new ArrayList<AutoAddScopeBuildItem>();
        var unremovableBeans = new ArrayList<UnremovableBeanBuildItem>();
        var model = ProcessorTestStubs.jpaModelWithPotentialCdiBeans(createSimple(potentialBeans));

        processor.registerBeans(
                additionalBeans::add,
                automaticBeans::add,
                unremovableBeans::add,
                capabilities,
                descriptors,
                model);

        return new BeanRegistration(additionalBeans, automaticBeans, unremovableBeans);
    }

    private static void assertDoNotRemoveBean(List<UnremovableBeanBuildItem> unremovableBeans, Class<?> beanClass) {
        assertThat(unremovableBeans).flatExtracting(UnremovableBeanBuildItem::getClassNames)
                .contains(createSimple(beanClass));
    }

    private static void assertBeanCanBeRemoved(List<UnremovableBeanBuildItem> unremovableBeans, Class<?> beanClass) {
        assertThat(unremovableBeans).flatExtracting(UnremovableBeanBuildItem::getClassNames)
                .doesNotContain(createSimple(beanClass));
    }

    private static boolean isEligibleForAutomaticScope(AutoAddScopeBuildItem item, Class<?> beanClass) throws IOException {
        Index indexWithBeans = Index.of(PotentialBean.class, UnrelatedBean.class);
        return item.test(indexWithBeans.getClassByName(beanClass), emptyList(), indexWithBeans);
    }

    static class PotentialBean {
    }

    static class UnrelatedBean {
    }

    private record BeanRegistration(List<AdditionalBeanBuildItem> additionalBeans,
            List<AutoAddScopeBuildItem> automaticBeans,
            List<UnremovableBeanBuildItem> unremovableBeans) {
    }
}

package io.quarkus.hibernate.orm.deployment;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.hibernate.orm.runtime.graal.RegisterServicesForReflectionFeature;
import io.quarkus.hibernate.orm.runtime.graal.RegisterStateManagementForReflectionFeature;

/**
 * Simulacrum of JPA bootstrap.
 * <p>
 * This does not address the proper integration with Hibernate
 * Rather prepare the path to providing the right metadata
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class HibernateOrmProcessor {

    /**
     * Collection of Hibernate annotations for which, if detected on interface, Hibernate processor generates repository.
     */
    public static final Set<Class<?>> HIBERNATE_REPOSITORY_ANNOTATIONS = Set.of(Find.class, HQL.class, SQL.class);

    @BuildStep
    NativeImageFeatureBuildItem registerServicesForReflection(BuildProducer<ServiceProviderBuildItem> services) {
        for (DotName serviceProvider : ClassNames.SERVICE_PROVIDERS) {
            services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(serviceProvider.toString()));
        }

        return new NativeImageFeatureBuildItem(RegisterServicesForReflectionFeature.class);
    }

    @BuildStep
    NativeImageFeatureBuildItem registerStateManagementForReflection() {
        return new NativeImageFeatureBuildItem(RegisterStateManagementForReflectionFeature.class);
    }

    @BuildStep
    void registerStrategyForReflection(
            BuildProducer<ReflectiveClassBuildItem> reflective) {

        // Hibernate ORM uses reflection at runtime two create these two strategies,
        // So we need this to support native-image
        // These strategies are only used in offline mode https://github.com/quarkusio/quarkus/pull/48130 so far
        // When Hibernate will support temporary table creation inside the `hbm2ddl` tool
        // https://hibernate.atlassian.net/browse/HHH-15525 the strategies won't be needed anymore and this can be removed
        reflective.produce(ReflectiveClassBuildItem.builder(
                LocalTemporaryTableInsertStrategy.class,
                LocalTemporaryTableMutationStrategy.class)
                .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                .methods().fields().build());
    }

    @BuildStep
    public void enrollBeanValidationTypeSafeActivatorForReflection(Capabilities capabilities,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR)) {
            // BeanValidationIntegrator is only added if this capability is present, see FastBootMetadataBuilder

            // Accessed in org.hibernate.boot.beanvalidation.BeanValidationIntegrator.loadTypeSafeActivatorClass
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder("org.hibernate.boot.beanvalidation.TypeSafeActivator")
                    .methods().fields().build());
            // Accessed in org.hibernate.boot.beanvalidation.BeanValidationIntegrator.isBeanValidationApiAvailable
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(BeanValidationIntegrator.JAKARTA_BV_CHECK_CLASS)
                    .constructors(false).build());
        }
    }

    /*
     * Enable reflection for methods annotated with @InjectService,
     * such as org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport.injectJdbcServices.
     */
    @BuildStep
    public void registerInjectServiceMethodsForReflection(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflective) {
        Set<String> classes = new HashSet<>();

        // Built-in service classes; can't rely on Jandex as Hibernate ORM is not indexed by default.
        ClassNames.ANNOTATED_WITH_INJECT_SERVICE.stream()
                .map(DotName::toString)
                .forEach(classes::add);

        // Integrators relying on @InjectService.
        index.getIndex().getAnnotations(ClassNames.INJECT_SERVICE).stream()
                .map(a -> a.target().asMethod().declaringClass().name().toString())
                .forEach(classes::add);

        if (!classes.isEmpty()) {
            reflective.produce(ReflectiveClassBuildItem.builder(classes.toArray(new String[0]))
                    .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                    .constructors(false).methods().build());
        }
    }
}

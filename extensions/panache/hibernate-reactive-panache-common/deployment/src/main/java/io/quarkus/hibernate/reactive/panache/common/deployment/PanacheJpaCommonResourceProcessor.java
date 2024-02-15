package io.quarkus.hibernate.reactive.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.reactive.panache.common.runtime.PanacheHibernateRecorder;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactionalInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.TestReactiveTransactionalInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.WithSessionInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.WithSessionOnDemandInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.WithTransactionInterceptor;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class PanacheJpaCommonResourceProcessor {

    private static final Logger LOG = Logger.getLogger(PanacheJpaCommonResourceProcessor.class);

    private static final String TEST_REACTIVE_TRANSACTION = "io.quarkus.test.TestReactiveTransaction";

    @BuildStep(onlyIf = IsTest.class)
    void testTx(BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        //generate the annotated interceptor with gizmo
        //all the logic is in the parent, but we don't have access to the
        //binding annotation here
        try (ClassCreator c = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer)).className(
                        TestReactiveTransactionalInterceptor.class.getName() + "Generated")
                .superClass(TestReactiveTransactionalInterceptor.class).build()) {
            c.addAnnotation(TEST_REACTIVE_TRANSACTION);
            c.addAnnotation(Interceptor.class.getName());
            c.addAnnotation(Priority.class).addValue("value", Interceptor.Priority.PLATFORM_BEFORE + 200);
        }
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(TestReactiveTransactionalInterceptor.class)
                .addBeanClass(TEST_REACTIVE_TRANSACTION).build());
    }

    @BuildStep
    void registerInterceptors(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClass(WithSessionOnDemandInterceptor.class);
        builder.addBeanClass(WithSessionInterceptor.class);
        builder.addBeanClass(ReactiveTransactionalInterceptor.class);
        builder.addBeanClass(WithTransactionInterceptor.class);
        additionalBeans.produce(builder.build());
    }

    @BuildStep
    void validateInterceptedMethods(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> errors) {
        List<DotName> bindings = List.of(DotNames.REACTIVE_TRANSACTIONAL, DotNames.WITH_SESSION,
                DotNames.WITH_SESSION_ON_DEMAND, DotNames.WITH_TRANSACTION);
        for (BeanInfo bean : validationPhase.getContext().beans().withAroundInvokeInterceptor()) {
            for (Entry<MethodInfo, Set<AnnotationInstance>> e : bean.getInterceptedMethodsBindings().entrySet()) {
                DotName returnTypeName = e.getKey().returnType().name();
                if (returnTypeName.equals(DotNames.UNI)) {
                    // Method returns Uni - no need to iterate over the bindings
                    continue;
                }
                validateBindings(bindings, e, errors);
            }
        }
    }

    @BuildStep
    void transformResourceMethods(CombinedIndexBuildItem index, Capabilities capabilities,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            // Custom request method designators are not supported
            List<DotName> designators = List.of(DotName.createSimple("jakarta.ws.rs.GET"),
                    DotName.createSimple("jakarta.ws.rs.HEAD"),
                    DotName.createSimple("jakarta.ws.rs.DELETE"), DotName.createSimple("jakarta.ws.rs.OPTIONS"),
                    DotName.createSimple("jakarta.ws.rs.PATCH"), DotName.createSimple("jakarta.ws.rs.POST"),
                    DotName.createSimple("jakarta.ws.rs.PUT"));
            List<DotName> bindings = List.of(DotNames.REACTIVE_TRANSACTIONAL, DotNames.WITH_SESSION,
                    DotNames.WITH_SESSION_ON_DEMAND, DotNames.WITH_TRANSACTION);

            // Collect all Panache entities and repositories
            Set<DotName> entities = new HashSet<>();
            for (ClassInfo subclass : index.getIndex().getAllKnownSubclasses(DotNames.PANACHE_ENTITY_BASE)) {
                if (!subclass.name().equals(DotNames.PANACHE_ENTITY)) {
                    entities.add(subclass.name());
                }
            }
            for (ClassInfo implementor : index.getIndex().getAllKnownImplementors(DotNames.PANACHE_KOTLIN_ENTITY_BASE)) {
                if (!implementor.name().equals(DotNames.PANACHE_KOTLIN_ENTITY)) {
                    entities.add(implementor.name());
                }
            }
            Set<DotName> repos = new HashSet<>();
            for (ClassInfo subclass : index.getIndex().getAllKnownImplementors(DotNames.PANACHE_REPOSITORY_BASE)) {
                if (!subclass.name().equals(DotNames.PANACHE_REPOSITORY)) {
                    repos.add(subclass.name());
                }
            }
            for (ClassInfo implementor : index.getIndex().getAllKnownImplementors(DotNames.PANACHE_KOTLIN_REPOSITORY_BASE)) {
                if (!implementor.name().equals(DotNames.PANACHE_KOTLIN_REPOSITORY)) {
                    repos.add(implementor.name());
                }
            }
            Set<DotName> entityReposUsers = new HashSet<>();
            for (DotName entity : entities) {
                for (ClassInfo user : index.getIndex().getKnownUsers(entity)) {
                    entityReposUsers.add(user.name());
                }
            }
            for (DotName repo : repos) {
                for (ClassInfo user : index.getIndex().getKnownUsers(repo)) {
                    entityReposUsers.add(user.name());
                }
            }

            annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
                @Override
                public boolean appliesTo(Kind kind) {
                    return kind == Kind.METHOD;
                }

                @Override
                public void transform(TransformationContext context) {
                    MethodInfo method = context.getTarget().asMethod();
                    Collection<AnnotationInstance> annotations = context.getAnnotations();
                    if (method.isSynthetic()
                            || Modifier.isStatic(method.flags())
                            || method.declaringClass().isInterface()
                            || !method.returnType().name().equals(DotNames.UNI)
                            || !entityReposUsers.contains(method.declaringClass().name())
                            || !Annotations.containsAny(annotations, designators)
                            || Annotations.containsAny(annotations, bindings)) {
                        return;
                    }
                    // Add @WithSessionOnDemand to a method that
                    // - is not static
                    // - is not synthetic
                    // - returns Uni
                    // - is declared in a class that uses a panache entity/repository
                    // - is annotated with @GET, @POST, @PUT, @DELETE ,@PATCH ,@HEAD or @OPTIONS
                    // - is not annotated with @ReactiveTransactional, @WithSession, @WithSessionOnDemand, or @WithTransaction
                    context.transform().add(DotNames.WITH_SESSION_ON_DEMAND).done();
                }
            }));
        }
    }

    @BuildStep
    void lookupNamedQueries(CombinedIndexBuildItem index,
            BuildProducer<PanacheNamedQueryEntityClassBuildStep> namedQueries,
            JpaModelBuildItem jpaModel) {
        for (String modelClass : jpaModel.getAllModelClassNames()) {
            // lookup for `@NamedQuery` on the hierarchy and produce NamedQueryEntityClassBuildStep
            Map<String, String> typeNamedQueries = new HashMap<>();
            lookupNamedQueries(index, DotName.createSimple(modelClass), typeNamedQueries);
            namedQueries.produce(new PanacheNamedQueryEntityClassBuildStep(modelClass, typeNamedQueries));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void buildNamedQueryMap(List<PanacheNamedQueryEntityClassBuildStep> namedQueryEntityClasses,
            PanacheHibernateRecorder panacheHibernateRecorder) {
        Map<String, Map<String, String>> namedQueryMap = new HashMap<>();
        for (PanacheNamedQueryEntityClassBuildStep entityNamedQueries : namedQueryEntityClasses) {
            namedQueryMap.put(entityNamedQueries.getClassName(), entityNamedQueries.getNamedQueries());
        }

        panacheHibernateRecorder.setNamedQueryMap(namedQueryMap);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void shutdown(ShutdownContextBuildItem shutdownContextBuildItem, PanacheHibernateRecorder panacheHibernateRecorder) {
        panacheHibernateRecorder.clear(shutdownContextBuildItem);
    }

    private void lookupNamedQueries(CombinedIndexBuildItem index, DotName name, Map<String, String> namedQueries) {
        ClassInfo classInfo = index.getComputingIndex().getClassByName(name);
        if (classInfo == null) {
            return;
        }

        List<AnnotationInstance> namedQueryInstances = classInfo.annotationsMap().get(DotNames.DOTNAME_NAMED_QUERY);
        if (namedQueryInstances != null) {
            for (AnnotationInstance namedQueryInstance : namedQueryInstances) {
                namedQueries.put(namedQueryInstance.value("name").asString(),
                        namedQueryInstance.value("query").asString());
            }
        }

        List<AnnotationInstance> namedQueriesInstances = classInfo.annotationsMap().get(DotNames.DOTNAME_NAMED_QUERIES);
        if (namedQueriesInstances != null) {
            for (AnnotationInstance namedQueriesInstance : namedQueriesInstances) {
                AnnotationValue value = namedQueriesInstance.value();
                AnnotationInstance[] nestedInstances = value.asNestedArray();
                for (AnnotationInstance nested : nestedInstances) {
                    namedQueries.put(nested.value("name").asString(), nested.value("query").asString());
                }
            }
        }

        // climb up the hierarchy of types
        if (!classInfo.superClassType().name().equals(JandexUtil.DOTNAME_OBJECT)) {
            Type superType = classInfo.superClassType();
            ClassInfo superClass = index.getComputingIndex().getClassByName(superType.name());
            if (superClass != null) {
                lookupNamedQueries(index, superClass.name(), namedQueries);
            }
        }
    }

    private void validateBindings(List<DotName> bindings, Entry<MethodInfo, Set<AnnotationInstance>> entry,
            BuildProducer<ValidationErrorBuildItem> errors) {
        for (DotName binding : bindings) {
            for (AnnotationInstance annotation : entry.getValue()) {
                if (annotation.name().equals(binding)) {
                    if (annotation.target().kind() == Kind.METHOD) {
                        errors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException(
                                        "A method annotated with @"
                                                + binding.withoutPackagePrefix()
                                                + " must return io.smallrye.mutiny.Uni: "
                                                + entry.getKey() + " declared on " + entry.getKey().declaringClass())));
                    } else {
                        LOG.debugf("Class-level binding %s will be ignored for method %s() declared on %s", binding,
                                entry.getKey().name(), entry.getKey().declaringClass());
                    }
                    return;
                }
            }
        }
    }
}

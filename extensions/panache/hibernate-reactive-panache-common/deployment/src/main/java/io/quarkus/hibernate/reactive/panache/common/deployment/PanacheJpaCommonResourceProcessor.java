package io.quarkus.hibernate.reactive.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

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
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.common.runtime.PanacheHibernateRecorder;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactionalInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.TestReactiveTransactionalInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.WithSessionInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.WithSessionOnDemandInterceptor;
import io.smallrye.mutiny.Uni;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class PanacheJpaCommonResourceProcessor {

    private static final DotName DOTNAME_NAMED_QUERY = DotName.createSimple(NamedQuery.class.getName());
    private static final DotName DOTNAME_NAMED_QUERIES = DotName.createSimple(NamedQueries.class.getName());
    private static final String TEST_REACTIVE_TRANSACTION = "io.quarkus.test.TestReactiveTransaction";

    private static final DotName REACTIVE_TRANSACTIONAL = DotName.createSimple(ReactiveTransactional.class.getName());
    private static final DotName WITH_SESSION_ON_DEMAND = DotName.createSimple(WithSessionOnDemand.class.getName());
    private static final DotName WITH_SESSION = DotName.createSimple(WithSession.class.getName());
    private static final DotName WITH_TRANSACTION = DotName.createSimple(WithTransaction.class.getName());
    private static final DotName UNI = DotName.createSimple(Uni.class.getName());
    private static final DotName PANACHE_ENTITY_BASE = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.PanacheEntityBase");
    private static final DotName PANACHE_ENTITY = DotName.createSimple("io.quarkus.hibernate.reactive.panache.PanacheEntity");
    private static final DotName PANACHE_KOTLIN_ENTITY_BASE = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase");
    private static final DotName PANACHE_KOTLIN_ENTITY = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity");

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
        additionalBeans.produce(builder.build());
    }

    @BuildStep
    void validateInterceptedMethods(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> errors) {
        List<DotName> bindings = List.of(REACTIVE_TRANSACTIONAL, WITH_SESSION, WITH_SESSION_ON_DEMAND, WITH_TRANSACTION);
        for (BeanInfo bean : validationPhase.getContext().beans().withAroundInvokeInterceptor()) {
            for (Entry<MethodInfo, Set<AnnotationInstance>> e : bean.getInterceptedMethodsBindings().entrySet()) {
                DotName returnTypeName = e.getKey().returnType().name();
                if (returnTypeName.equals(UNI)) {
                    // Method returns Uni - no need to iterate over the bindings
                    continue;
                }
                if (Annotations.containsAny(e.getValue(), bindings)) {
                    errors.produce(new ValidationErrorBuildItem(
                            new IllegalStateException(
                                    "A method annotated with "
                                            + bindings.stream().map(b -> "@" + b.withoutPackagePrefix())
                                                    .collect(Collectors.toList())
                                            + " must return Uni: "
                                            + e.getKey() + " declared on " + e.getKey().declaringClass())));
                }
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
            List<DotName> bindings = List.of(REACTIVE_TRANSACTIONAL, WITH_SESSION, WITH_SESSION_ON_DEMAND, WITH_TRANSACTION);

            // Collect all panache entities
            Set<DotName> entities = new HashSet<>();
            for (ClassInfo subclass : index.getIndex().getAllKnownSubclasses(PANACHE_ENTITY_BASE)) {
                if (!subclass.name().equals(PANACHE_ENTITY)) {
                    entities.add(subclass.name());
                }
            }
            for (ClassInfo subclass : index.getIndex().getAllKnownImplementors(PANACHE_KOTLIN_ENTITY_BASE)) {
                if (!subclass.name().equals(PANACHE_KOTLIN_ENTITY)) {
                    entities.add(subclass.name());
                }
            }
            Set<DotName> entityUsers = new HashSet<>();
            for (DotName entity : entities) {
                for (ClassInfo user : index.getIndex().getKnownUsers(entity)) {
                    entityUsers.add(user.name());
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
                            || !method.returnType().name().equals(UNI)
                            || !entityUsers.contains(method.declaringClass().name())
                            || !Annotations.containsAny(annotations, designators)
                            || Annotations.containsAny(annotations, bindings)) {
                        return;
                    }
                    // Add @WithSessionOnDemand to a method that
                    // - is not static
                    // - is not synthetic
                    // - returns Uni
                    // - is declared in a class that uses a panache entity
                    // - is annotated with @GET, @POST, @PUT, @DELETE ,@PATCH ,@HEAD or @OPTIONS
                    // - is not annotated with @ReactiveTransactional, @WithSession, @WithSessionOnDemand, or @WithTransaction
                    context.transform().add(WITH_SESSION_ON_DEMAND).done();
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
            Set<String> typeNamedQueries = new HashSet<>();
            lookupNamedQueries(index, DotName.createSimple(modelClass), typeNamedQueries);
            namedQueries.produce(new PanacheNamedQueryEntityClassBuildStep(modelClass, typeNamedQueries));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void buildNamedQueryMap(List<PanacheNamedQueryEntityClassBuildStep> namedQueryEntityClasses,
            PanacheHibernateRecorder panacheHibernateRecorder) {
        Map<String, Set<String>> namedQueryMap = new HashMap<>();
        for (PanacheNamedQueryEntityClassBuildStep entityNamedQueries : namedQueryEntityClasses) {
            namedQueryMap.put(entityNamedQueries.getClassName(), entityNamedQueries.getNamedQueries());
        }

        panacheHibernateRecorder.setNamedQueryMap(namedQueryMap);
    }

    private void lookupNamedQueries(CombinedIndexBuildItem index, DotName name, Set<String> namedQueries) {
        ClassInfo classInfo = index.getComputingIndex().getClassByName(name);
        if (classInfo == null) {
            return;
        }

        List<AnnotationInstance> namedQueryInstances = classInfo.annotationsMap().get(DOTNAME_NAMED_QUERY);
        if (namedQueryInstances != null) {
            for (AnnotationInstance namedQueryInstance : namedQueryInstances) {
                namedQueries.add(namedQueryInstance.value("name").asString());
            }
        }

        List<AnnotationInstance> namedQueriesInstances = classInfo.annotationsMap().get(DOTNAME_NAMED_QUERIES);
        if (namedQueriesInstances != null) {
            for (AnnotationInstance namedQueriesInstance : namedQueriesInstances) {
                AnnotationValue value = namedQueriesInstance.value();
                AnnotationInstance[] nestedInstances = value.asNestedArray();
                for (AnnotationInstance nested : nestedInstances) {
                    namedQueries.add(nested.value("name").asString());
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
}

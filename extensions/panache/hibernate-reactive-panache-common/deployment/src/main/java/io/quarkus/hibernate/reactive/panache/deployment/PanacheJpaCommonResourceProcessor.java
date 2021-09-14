package io.quarkus.hibernate.reactive.panache.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.interceptor.Interceptor;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.reactive.panache.common.runtime.PanacheHibernateRecorder;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactionalInterceptor;
import io.quarkus.hibernate.reactive.panache.common.runtime.TestReactiveTransactionalInterceptor;

public final class PanacheJpaCommonResourceProcessor {

    private static final DotName DOTNAME_NAMED_QUERY = DotName.createSimple(NamedQuery.class.getName());
    private static final DotName DOTNAME_NAMED_QUERIES = DotName.createSimple(NamedQueries.class.getName());
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
    void registerInterceptor(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClass(ReactiveTransactionalInterceptor.class);
        additionalBeans.produce(builder.build());
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
        ClassInfo classInfo = index.getIndex().getClassByName(name);
        if (classInfo == null) {
            return;
        }

        List<AnnotationInstance> namedQueryInstances = classInfo.annotations().get(DOTNAME_NAMED_QUERY);
        if (namedQueryInstances != null) {
            for (AnnotationInstance namedQueryInstance : namedQueryInstances) {
                namedQueries.add(namedQueryInstance.value("name").asString());
            }
        }

        List<AnnotationInstance> namedQueriesInstances = classInfo.annotations().get(DOTNAME_NAMED_QUERIES);
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
            ClassInfo superClass = index.getIndex().getClassByName(superType.name());
            if (superClass != null) {
                lookupNamedQueries(index, superClass.name(), namedQueries);
            }
        }
    }
}

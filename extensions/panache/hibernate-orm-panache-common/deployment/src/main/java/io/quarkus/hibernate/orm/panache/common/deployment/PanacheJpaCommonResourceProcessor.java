package io.quarkus.hibernate.orm.panache.common.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.panache.common.runtime.PanacheHibernateRecorder;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class PanacheJpaCommonResourceProcessor {

    private static final DotName DOTNAME_NAMED_QUERY = DotName.createSimple(NamedQuery.class.getName());
    private static final DotName DOTNAME_NAMED_QUERIES = DotName.createSimple(NamedQueries.class.getName());

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

    private void lookupNamedQueries(CombinedIndexBuildItem index, DotName name, Map<String, String> namedQueries) {
        ClassInfo classInfo = index.getComputingIndex().getClassByName(name);
        if (classInfo == null) {
            return;
        }

        List<AnnotationInstance> namedQueryInstances = classInfo.annotationsMap().get(DOTNAME_NAMED_QUERY);
        if (namedQueryInstances != null) {
            for (AnnotationInstance namedQueryInstance : namedQueryInstances) {
                namedQueries.put(namedQueryInstance.value("name").asString(), namedQueryInstance.value("query").asString());
            }
        }

        List<AnnotationInstance> namedQueriesInstances = classInfo.annotationsMap().get(DOTNAME_NAMED_QUERIES);
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
}

package io.quarkus.hibernate.orm.panache.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.panache.common.runtime.PanacheHibernateRecorder;

public final class PanacheJpaCommonResourceProcessor {

    private static final DotName DOTNAME_NAMED_QUERY = DotName.createSimple(NamedQuery.class.getName());
    private static final DotName DOTNAME_NAMED_QUERIES = DotName.createSimple(NamedQueries.class.getName());

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

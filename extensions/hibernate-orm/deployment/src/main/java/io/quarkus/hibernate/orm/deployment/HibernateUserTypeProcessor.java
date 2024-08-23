package io.quarkus.hibernate.orm.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class HibernateUserTypeProcessor {
    private static final String TYPE_VALUE = "type";
    private static final String TYPE_CLASS_VALUE = "typeClass";

    @BuildStep
    public void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();

        final Set<String> userTypes = new HashSet<>();

        Collection<AnnotationInstance> typeAnnotationInstances = index.getAnnotations(ClassNames.TYPE);

        for (AnnotationInstance typeAnnotationInstance : typeAnnotationInstances) {
            final AnnotationValue typeValue = typeAnnotationInstance.value(TYPE_VALUE);
            if (typeValue == null) {
                continue;
            }

            final String type = typeValue.asString();
            final DotName className = DotName.createSimple(type);
            if (index.getClassByName(className) == null) {
                continue; // Either already registered through TypeDef annotation scanning or not present in the index
            }
            userTypes.add(type);
        }

        if (!userTypes.isEmpty()) {
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(userTypes.toArray(new String[] {})).methods().fields().build());
        }
    }

    private Set<String> getUserTypes(Collection<AnnotationInstance> typeDefinitionAnnotationInstances) {
        final Set<String> userTypes = new HashSet<>();

        for (AnnotationInstance typeDefAnnotationInstance : typeDefinitionAnnotationInstances) {
            final AnnotationValue typeClassValue = typeDefAnnotationInstance.value(TYPE_CLASS_VALUE);
            if (typeClassValue == null) {
                continue;
            }

            final String typeClass = typeClassValue.asClass().name().toString();
            userTypes.add(typeClass);
        }

        return userTypes;
    }
}

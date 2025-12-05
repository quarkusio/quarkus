package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.smallrye.common.annotation.Identifier;

public final class ElasticsearchClientProcessorUtil {

    private ElasticsearchClientProcessorUtil() {
    }

    private static final DotName DEFAULT_ANNOTATION = DotName.createSimple(Default.class.getName());
    public static final DotName IDENTIFIER_ELASTICSEARCH_CLIENT_NAME_ANNOTATION = DotName
            .createSimple(Identifier.class.getName());

    /**
     * Collect referenced names for a given type of Elasticsearch client:
     * <ul>
     * <li>All injected clients with the @Default or @Identifier qualifiers</li>
     * <li>All configuration classes that are expected to target a given client,
     * e.g. @ElasticsearchClientConfig</li>
     * </ul>
     */
    public static Set<String> collectReferencedClientNames(CombinedIndexBuildItem indexBuildItem,
            BeanRegistrationPhaseBuildItem registrationPhase,
            Set<DotName> clientTypeNames, Set<DotName> configAnnotationNames) {
        Set<String> referencedNames = new HashSet<>();
        // Always start with the default:
        referencedNames.add(ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME);
        IndexView indexView = indexBuildItem.getIndex();
        for (DotName annotationName : configAnnotationNames) {
            for (AnnotationInstance annotation : indexView.getAnnotations(annotationName)) {
                AnnotationValue value = annotation.value();
                if (value == null) {
                    referencedNames.add(ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME);
                } else {
                    referencedNames.add(value.asString());
                }
            }
        }
        for (InjectionPointInfo injectionPoint : registrationPhase.getInjectionPoints()) {
            DotName injectionPointType = injectionPoint.getRequiredType().name();
            if (!clientTypeNames.contains(injectionPointType)) {
                // We only care about injections of our client type(s)
                continue;
            }
            if (injectionPoint.hasDefaultedQualifier()) {
                referencedNames.add(ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME);
                continue;
            }
            for (AnnotationInstance requiredQualifier : injectionPoint.getRequiredQualifiers()) {
                if (requiredQualifier.name().equals(DEFAULT_ANNOTATION)) {
                    referencedNames.add(ElasticsearchClientBeanUtil.DEFAULT_ELASTICSEARCH_CLIENT_NAME);
                }
                if (requiredQualifier.name().equals(IDENTIFIER_ELASTICSEARCH_CLIENT_NAME_ANNOTATION)) {
                    referencedNames.add(requiredQualifier.value().asString());
                }
            }
        }
        return referencedNames;
    }
}

package io.quarkus.elasticsearch.restclient.common.deployment;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.elasticsearch.restclient.common.ElasticsearchClientName;
import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import javax.enterprise.inject.Default;
import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;

public final class ElasticsearchClientProcessorUtil {

    private ElasticsearchClientProcessorUtil() {
    }

    private static final DotName DEFAULT_ANNOTATION = DotName.createSimple(Default.class.getName());
    private static final DotName NAMED_ANNOTATION = DotName.createSimple(Named.class.getName());
    public static final DotName ELASTICSEARCH_CLIENT_NAME_ANNOTATION = DotName
            .createSimple(ElasticsearchClientName.class.getName());

    /**
     * Collect referenced names for a given type of Elasticsearch client:
     * <ul>
     *     <li>All injected clients with the @Default, @Named or @ElasticsearchClientName qualifiers</li>
     *     <li>All configuration classes that are expected to target a given client,
     *     e.g. @ElasticsearchClientConfig</li>
     * </ul>
     */
    public static Set<String> collectReferencedClientNames(CombinedIndexBuildItem indexBuildItem,
            BeanRegistrationPhaseBuildItem registrationPhase,
            Set<DotName> clientTypeNames, Set<DotName> configAnnotationNames) {
        Set<String> referencedNames = new HashSet<>();
        IndexView indexView = indexBuildItem.getIndex();
        for (DotName annotationName : configAnnotationNames) {
            for (AnnotationInstance annotation : indexView.getAnnotations(annotationName)) {
                referencedNames.add(annotation.value().asString());
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
                } else if (requiredQualifier.name().equals(NAMED_ANNOTATION) && requiredQualifier.value() != null) {
                    referencedNames.add(requiredQualifier.value().asString());
                }
                if (requiredQualifier.name().equals(ELASTICSEARCH_CLIENT_NAME_ANNOTATION)) {
                    referencedNames.add(requiredQualifier.value().asString());
                }
            }
        }
        return referencedNames;
    }
}

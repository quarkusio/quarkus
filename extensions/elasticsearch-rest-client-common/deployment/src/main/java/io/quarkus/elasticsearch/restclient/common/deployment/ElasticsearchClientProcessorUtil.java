package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.elasticsearch.restclient.common.runtime.ElasticsearchClientBeanUtil;
import io.smallrye.common.annotation.Identifier;

public final class ElasticsearchClientProcessorUtil {

    private ElasticsearchClientProcessorUtil() {
    }

    private static final DotName DEFAULT_ANNOTATION = DotName.createSimple(Default.class.getName());
    public static final DotName IDENTIFIER_ELASTICSEARCH_CLIENT_NAME_ANNOTATION = DotName
            .createSimple(Identifier.class.getName());

    /**
     * Collect referenced client names by scanning injection points for a given type of Elasticsearch client.
     * Returns client names found via @Default or @Identifier qualifiers on injection points.
     */
    public static Set<String> collectReferencedClientNames(
            BeanRegistrationPhaseBuildItem registrationPhase,
            Set<DotName> clientTypeNames) {
        Set<String> referencedNames = new HashSet<>();
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

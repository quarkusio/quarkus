package io.quarkus.resteasy.reactive.data.hibernate.deployment;

import java.util.Map;

import jakarta.data.Direction;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.reactive.data.hibernate.runtime.DirectionParamExtractor;
import io.quarkus.resteasy.reactive.data.hibernate.runtime.JakartaDataObjectMapperCustomizer;
import io.quarkus.resteasy.reactive.data.hibernate.runtime.LimitParamExtractor;
import io.quarkus.resteasy.reactive.data.hibernate.runtime.OrderParamExtractor;
import io.quarkus.resteasy.reactive.data.hibernate.runtime.PageRequestParamExtractor;
import io.quarkus.resteasy.reactive.data.hibernate.runtime.SortParamExtractor;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;

public class RestDataHibernateProcessor {

    private static final DotName PAGE_REQUEST = DotName.createSimple(PageRequest.class);
    private static final DotName SORT = DotName.createSimple(Sort.class);
    private static final DotName ORDER = DotName.createSimple(Order.class);
    private static final DotName LIMIT = DotName.createSimple(Limit.class);
    private static final DotName DIRECTION = DotName.createSimple(Direction.class);
    private static final DotName REST_QUERY = DotName.createSimple(RestQuery.class);

    @BuildStep
    AdditionalBeanBuildItem registerJacksonCustomizer() {
        return AdditionalBeanBuildItem.unremovableOf(JakartaDataObjectMapperCustomizer.class);
    }

    @BuildStep
    MethodScannerBuildItem jakartaDataParamScanner() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public ParameterExtractor handleCustomParameter(Type paramType, Map<DotName, AnnotationInstance> annotations,
                    boolean field, Map<String, Object> methodContext) {
                DotName name = paramType.name();
                String parameterName = extractParameterName(annotations, name);

                if (name.equals(PAGE_REQUEST)) {
                    return new PageRequestParamExtractor(parameterName);
                }
                if (name.equals(SORT)) {
                    return new SortParamExtractor(parameterName);
                }
                if (name.equals(ORDER)) {
                    return new OrderParamExtractor(parameterName);
                }
                if (name.equals(LIMIT)) {
                    return new LimitParamExtractor(parameterName);
                }
                if (name.equals(DIRECTION)) {
                    return new DirectionParamExtractor(parameterName);
                }
                return null;
            }

            private String extractParameterName(Map<DotName, AnnotationInstance> annotations, DotName paramType) {
                AnnotationInstance restQuery = annotations.get(REST_QUERY);
                if (restQuery != null) {
                    AnnotationValue value = restQuery.value();
                    if (value != null && !value.asString().isEmpty()) {
                        return value.asString();
                    }
                }
                return getDefaultParameterName(paramType);
            }

            private String getDefaultParameterName(DotName paramType) {
                if (paramType.equals(PAGE_REQUEST)) {
                    return "page";
                }
                if (paramType.equals(SORT) || paramType.equals(ORDER)) {
                    return "sort";
                }
                if (paramType.equals(LIMIT)) {
                    return "limit";
                }
                if (paramType.equals(DIRECTION)) {
                    return "direction";
                }
                return "param";
            }
        });
    }
}

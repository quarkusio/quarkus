package io.quarkus.resteasy.reactive.data.hibernate.deployment;

import java.util.Map;

import jakarta.data.Direction;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
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
                if (name.equals(PAGE_REQUEST)) {
                    return new PageRequestParamExtractor();
                }
                if (name.equals(SORT)) {
                    return new SortParamExtractor();
                }
                if (name.equals(ORDER)) {
                    return new OrderParamExtractor();
                }
                if (name.equals(LIMIT)) {
                    return new LimitParamExtractor();
                }
                if (name.equals(DIRECTION)) {
                    return new DirectionParamExtractor();
                }
                return null;
            }
        });
    }
}

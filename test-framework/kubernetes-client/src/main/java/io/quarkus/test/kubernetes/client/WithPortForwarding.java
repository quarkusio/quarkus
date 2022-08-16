package io.quarkus.test.kubernetes.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(value = PortForwardingTestResource.class, restrictToAnnotatedClass = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithPortForwarding {
    Pod pod();

    Port port() default @Port;

    int localPort() default AnnotationConstants.UNSET_INT_VALUE;

    String namespace() default AnnotationConstants.UNSET_STRING_VALUE;

    @interface Pod {
        String labelSelector() default AnnotationConstants.UNSET_STRING_VALUE;

        LabelValue[] labelValues() default {};
        
        FieldSelector[] fieldSelectors() default {};

        int podIndex() default 0;
    }

    @interface FieldSelector {
        enum Operator {
            eq,
            neq
        }

        String key();

        Operator operator() default Operator.eq;

        String value();
    }

    @interface LabelValue {
        String key();
        String value() default AnnotationConstants.UNSET_STRING_VALUE;
    }

    @interface Port {
        int port() default AnnotationConstants.UNSET_INT_VALUE;

        int containerIndex() default 0;

        int portIndex() default 0;
    }
}

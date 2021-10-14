package io.quarkus.resteasy.reactive.hibernate.reactive.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.server.core.parameters.NullParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.reactive.hibernate.reactive.runtime.HibernateReactiveCustomizer;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.smallrye.mutiny.Uni;

public class ResteasyReactiveHibernateReactiveProcessor {

    public static final DotName SESSION = DotName.createSimple(Mutiny.Session.class.getName());
    public static final DotName TRANSACTION = DotName.createSimple(Mutiny.Transaction.class.getName());
    public static final DotName UNI = DotName.createSimple(Uni.class.getName());

    @BuildStep
    MethodScannerBuildItem scanner(BuildProducer<UnremovableBeanBuildItem> unremovableBeanBuildItemBuildProducer) {
        unremovableBeanBuildItemBuildProducer.produce(UnremovableBeanBuildItem.beanTypes(Mutiny.SessionFactory.class));
        return new MethodScannerBuildItem(new MethodScanner() {
            @SuppressWarnings("unchecked")
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                int session = -1;
                int tx = -1;
                List<Type> parameters = method.parameters();
                for (int j = 0; j < parameters.size(); j++) {
                    Type type = parameters.get(j);
                    if (type.name().equals(SESSION)) {
                        session = j;
                    } else if (type.name().equals(TRANSACTION)) {
                        tx = j;
                    }
                }
                if (session >= 0 || tx >= 0) {

                    if (!method.returnType().name().equals(UNI)) {
                        throw new RuntimeException(
                                "Method " + method + " must return a Uni to inject Mutiny.Session and Mutiny.Transaction");
                    }
                    return Collections.singletonList(new HibernateReactiveCustomizer(session, tx));
                }
                return Collections.emptyList();
            }

            @Override
            public ParameterExtractor handleCustomParameter(Type paramType, Map<DotName, AnnotationInstance> annotations,
                    boolean field, Map<String, Object> methodContext) {
                //look for methods that take a Continuation, these are suspendable and need to be handled differently
                if (paramType.name().equals(SESSION) || paramType.name().equals(TRANSACTION)) {
                    return new NullParamExtractor();
                }
                return null;
            }

        });
    }
}

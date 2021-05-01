package io.quarkus.resteasy.reactive.server.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.HashUtil;
import org.jboss.resteasy.reactive.server.core.parameters.NullParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.runtime.kotlin.ApplicationCoroutineScope;
import org.jboss.resteasy.reactive.server.runtime.kotlin.CoroutineEndpointInvoker;
import org.jboss.resteasy.reactive.server.runtime.kotlin.CoroutineInvocationHandlerFactory;
import org.jboss.resteasy.reactive.server.runtime.kotlin.CoroutineMethodProcessor;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class KotlinCoroutineIntegrationProcessor {

    static final DotName CONTINUATION = DotName.createSimple("kotlin.coroutines.Continuation");
    public static final String NAME = KotlinCoroutineIntegrationProcessor.class.getName();

    @BuildStep
    AdditionalBeanBuildItem produceCoroutineScope() {
        return AdditionalBeanBuildItem.builder().addBeanClass(ApplicationCoroutineScope.class).setUnremovable().build();
    }

    @BuildStep
    AdditionalBeanBuildItem produceCoroutineInvocationHandlerFactory() {
        return AdditionalBeanBuildItem.builder().addBeanClass(CoroutineInvocationHandlerFactory.class).setUnremovable().build();
    }

    @BuildStep
    MethodScannerBuildItem scanner() {
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                if (methodContext.containsKey(NAME)) {
                    //method is suspendable, we need to handle the invocation differently
                    ResteasyReactiveRecorder recorder = (ResteasyReactiveRecorder) methodContext
                            .get(ResteasyReactiveRecorder.class.getName());
                    CoroutineMethodProcessor processor = new CoroutineMethodProcessor(createCoroutineInvoker(
                            method.declaringClass(), method,
                            (BuildProducer<GeneratedClassBuildItem>) methodContext.get(GeneratedClassBuildItem.class.getName()),
                            recorder));
                    return Collections.singletonList(processor);
                }
                return Collections.emptyList();
            }

            @Override
            public ParameterExtractor handleCustomParameter(Type paramType, Map<DotName, AnnotationInstance> annotations,
                    boolean field, Map<String, Object> methodContext) {
                //look for methods that take a Continuation, these are suspendable and need to be handled differently
                if (paramType.name().equals(CONTINUATION)) {
                    methodContext.put(NAME, true);
                    return new NullParamExtractor();
                }
                return null;
            }
        });
    }

    /**
     * This method generates the same invocation code as for the standard invoker but also passes along the implicit
     * {@code Continuation} argument provided by kotlinc and the coroutines library.
     *
     * @see QuarkusInvokerFactory#create(ResourceMethod, ClassInfo, MethodInfo)
     */
    private Supplier<EndpointInvoker> createCoroutineInvoker(ClassInfo currentClassInfo,
            MethodInfo info, BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            ResteasyReactiveRecorder recorder) {
        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(info.name())
                .append(info.returnType());
        for (Type t : info.parameters()) {
            sigBuilder.append(t);
        }
        String baseName = currentClassInfo.name() + "$quarkuscoroutineinvoker$" + info.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());
        //this is very similar to the existing impl, except it passes through a continuation as an additional argument
        try (ClassCreator classCreator = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                Object.class.getName(), CoroutineEndpointInvoker.class.getName())) {

            try (MethodCreator mc = classCreator.getMethodCreator("invoke", Object.class, Object.class, Object[].class)) {
                mc.throwException(IllegalStateException.class, "Incorrect invoker used for Kotlin suspendable method");
            }

            try (MethodCreator mc = classCreator.getMethodCreator("invokeCoroutine", Object.class, Object.class, Object[].class,
                    CONTINUATION.toString())) {
                ResultHandle[] args = new ResultHandle[info.parameters().size()];
                ResultHandle array = mc.getMethodParam(1);
                for (int i = 0; i < info.parameters().size() - 1; ++i) {
                    args[i] = mc.readArrayValue(array, i);
                }
                args[args.length - 1] = mc.getMethodParam(2);
                ResultHandle res;
                if (Modifier.isInterface(currentClassInfo.flags())) {
                    res = mc.invokeInterfaceMethod(info, mc.getMethodParam(0), args);
                } else {
                    res = mc.invokeVirtualMethod(info, mc.getMethodParam(0), args);
                }
                if (info.returnType().kind() == Type.Kind.VOID) {
                    mc.returnValue(mc.loadNull());
                } else {
                    mc.returnValue(res);
                }
            }

        }
        return recorder.invoker(baseName);
    }
}

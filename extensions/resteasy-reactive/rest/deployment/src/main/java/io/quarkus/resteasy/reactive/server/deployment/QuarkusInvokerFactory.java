package io.quarkus.resteasy.reactive.server.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.HashUtil;
import org.jboss.resteasy.reactive.server.processor.EndpointInvokerFactory;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;

public class QuarkusInvokerFactory implements EndpointInvokerFactory {

    private final Predicate<String> applicationClassPredicate;
    final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    final ResteasyReactiveRecorder recorder;

    public QuarkusInvokerFactory(Predicate<String> applicationClassPredicate,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            ResteasyReactiveRecorder recorder) {
        this.applicationClassPredicate = applicationClassPredicate;
        this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
        this.recorder = recorder;
    }

    @Override
    public Supplier<EndpointInvoker> create(ResourceMethod method, ClassInfo currentClassInfo, MethodInfo info) {

        String endpointIdentifier = info.toString() +
                method.getHttpMethod() +
                method.getPath() +
                Arrays.toString(method.getConsumes()) +
                Arrays.toString(method.getProduces());

        String baseName = currentClassInfo.name() + "$quarkusrestinvoker$" + method.getName() + "_"
                + HashUtil.sha1(endpointIdentifier);
        try (ClassCreator classCreator = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer,
                        applicationClassPredicate.test(currentClassInfo.name().toString())),
                baseName, null,
                Object.class.getName(), EndpointInvoker.class.getName())) {
            MethodCreator mc = classCreator.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
            ResultHandle[] args = new ResultHandle[method.getParameters().length];
            ResultHandle array = mc.getMethodParam(1);
            for (int i = 0; i < method.getParameters().length; ++i) {
                args[i] = mc.readArrayValue(array, i);
            }
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
        return recorder.invoker(baseName);
    }
}

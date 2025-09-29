package io.quarkus.resteasy.reactive.server.deployment;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.HashUtil;
import org.jboss.resteasy.reactive.server.processor.EndpointInvokerFactory;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;

import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRecorder;

public class QuarkusInvokerFactory implements EndpointInvokerFactory {

    private final Predicate<String> applicationClassPredicate;
    final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;
    final ResteasyReactiveRecorder recorder;

    private final Map<String, Supplier<EndpointInvoker>> generatedInvokers = new HashMap<>();

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
        if (generatedInvokers.containsKey(baseName)) {
            return generatedInvokers.get(baseName);
        }
        ClassOutput classOutput = new GeneratedClassGizmo2Adaptor(generatedClassBuildItemBuildProducer, null,
                applicationClassPredicate.test(currentClassInfo.name().toString()));
        Gizmo g = Gizmo.create(classOutput);
        g.class_(baseName, cc -> {
            cc.defaultConstructor();
            cc.implements_(EndpointInvoker.class);
            cc.method("invoke", mc -> {
                ParamVar resourceParam = mc.parameter("resource", Object.class);
                ParamVar resourceMethodArgsParam = mc.parameter("args", Object[].class);
                mc.returning(Object.class);
                mc.body(bc -> {
                    List<Expr> args = new ArrayList<>(method.getParameters().length);
                    Expr res;
                    for (int i = 0; i < method.getParameters().length; ++i) {
                        args.add(resourceMethodArgsParam.elem(i));
                    }
                    if (Modifier.isInterface(currentClassInfo.flags())) {
                        res = bc.invokeInterface(methodDescOf(info), resourceParam, args);
                    } else {
                        res = bc.invokeVirtual(methodDescOf(info), resourceParam, args);
                    }
                    if (info.returnType().kind() == Type.Kind.VOID) {
                        bc.returnNull();
                    } else {
                        bc.return_(res);
                    }
                });
            });
        });
        var result = recorder.invoker(baseName);
        generatedInvokers.put(baseName, result);
        return result;
    }

}

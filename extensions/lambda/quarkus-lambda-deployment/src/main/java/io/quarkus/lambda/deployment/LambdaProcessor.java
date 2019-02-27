package io.quarkus.lambda.deployment;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.function.Consumer;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.objectweb.asm.Opcodes;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import io.quarkus.lambda.runtime.LambdaServlet;
import io.quarkus.undertow.ServletBuildItem;

public final class LambdaProcessor {
    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());

    @BuildStep
    public void servlets(CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<ServletBuildItem> servletProducer,
            Consumer<GeneratedClassBuildItem> classConsumer,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        final ClassOutput classOutput = (name, data) -> classConsumer.accept(new GeneratedClassBuildItem(true, name, data));

        for (ClassInfo info : combinedIndexBuildItem.getIndex().getAllKnownImplementors(REQUEST_HANDLER)) {
            final DotName name = info.name();

            final String lambda = name.toString();
            final String mapping = name.local();
            final String servletName = name.toString() + "Lambda";

            servletProducer.produce(ServletBuildItem.builder(mapping, servletName)
                    .setLoadOnStartup(1)
                    .addMapping("/" + mapping)
                    .build());

            try (final ClassCreator creator = new ClassCreator(classOutput, servletName, null, LambdaServlet.class.getName())) {
                try (MethodCreator ctor = creator.getMethodCreator("<init>", "void")) {
                    ctor.setModifiers(Opcodes.ACC_PUBLIC);
                    ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(LambdaServlet.class, Class.class), ctor.getThis(),
                            ctor.loadClass(lambda));
                    ctor.returnValue(null);
                }
            }

            final List<MethodInfo> list = info.methods().stream()
                    .filter(methodInfo -> methodInfo.name().equals("handleRequest"))
                    .collect(toList());
            final MethodInfo methodInfo = list.get(0);
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(methodInfo));

            final Type firstParam = methodInfo.parameters().get(0).asClassType();
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, firstParam.name().toString()));
        }
    }
}

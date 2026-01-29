package io.quarkus.reactive.datasource.deployment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.reactive.datasource.runtime.StealingHelper;
import io.vertx.core.Future;

public class StealingPoolWrapperGenerator {

    /**
     * Generates a bytecode wrapper for the specific Pool interface (e.g., PgPool, MySQLPool).
     *
     * @param generatedClasses Producer for generated class files
     * @param reflectiveClasses Producer for native image reflection config
     * @param poolInterface The target interface to implement (e.g., io.vertx.pgclient.PgPool)
     * @param wrapperClassName The fully qualified name for the generated wrapper
     */
    public static void generate(BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Class<?> poolInterface,
            String wrapperClassName) {

        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClasses, true))
                .className(wrapperClassName)
                .interfaces(poolInterface)
                .build()) {

            FieldDescriptor delegateField = cc.getFieldCreator("delegate", poolInterface)
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL).getFieldDescriptor();

            FieldDescriptor nameField = cc.getFieldCreator("datasourceName", String.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL).getFieldDescriptor();

            FieldDescriptor helperField = cc.getFieldCreator("helper", StealingHelper.class)
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL).getFieldDescriptor();

            try (MethodCreator ctor = cc.getMethodCreator("<init>", void.class, poolInterface, String.class,
                    StealingHelper.class)) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());

                ctor.writeInstanceField(delegateField, ctor.getThis(), ctor.getMethodParam(0));
                ctor.writeInstanceField(nameField, ctor.getThis(), ctor.getMethodParam(1));
                ctor.writeInstanceField(helperField, ctor.getThis(), ctor.getMethodParam(2));
                ctor.returnValue(null);
            }

            for (Method method : poolInterface.getMethods()) {

                // SPECIAL HANDLING: withConnection / withTransaction
                // If these are default methods, we skip generating them.
                // This causes the wrapper to inherit the interface's default implementation,
                // which internally calls 'this.getConnection()', ensuring our interception logic is triggered.
                if (method.isDefault() &&
                        (method.getName().equals("withConnection") || method.getName().equals("withTransaction"))) {
                    continue;
                }

                MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(
                        method.getDeclaringClass(),
                        method.getName(),
                        method.getReturnType(),
                        method.getParameterTypes());

                try (MethodCreator mc = cc.getMethodCreator(methodDescriptor)) {
                    ResultHandle delegate = mc.readInstanceField(delegateField, mc.getThis());

                    // Load arguments
                    ResultHandle[] args = new ResultHandle[method.getParameterCount()];
                    for (int i = 0; i < method.getParameterCount(); i++) {
                        args[i] = mc.getMethodParam(i);
                    }

                    // Invoke delegate
                    ResultHandle result = mc.invokeInterfaceMethod(methodDescriptor, delegate, args);

                    // INTERCEPT: getConnection()
                    // We only intercept the no-arg version that returns a Future.
                    if (method.getName().equals("getConnection")
                            && method.getParameterCount() == 0
                            && Future.class.isAssignableFrom(method.getReturnType())) {

                        ResultHandle nameVal = mc.readInstanceField(nameField, mc.getThis());
                        ResultHandle helperVal = mc.readInstanceField(helperField, mc.getThis());

                        // Call helper.wrap(result, datasourceName)
                        result = mc.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(StealingHelper.class, "wrap", Future.class, Future.class,
                                        String.class),
                                helperVal, result, nameVal);
                    }

                    mc.returnValue(result);
                }
            }
        }

        // Register for Reflection (Required for Native Image)
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(wrapperClassName).constructors(true).build());
    }
}
package io.quarkus.redis.deployment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

import javax.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.lettuce.core.dynamic.Commands;
import io.lettuce.core.dynamic.intercept.MethodInvocation;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.redis.runtime.QuarkusRedisCommandFactory;

class RedisCommandsClassGenerator {
    /**
     * Generate a CommandsImpl class implementing subclasses of {@link Commands}.
     * The generated bean delegates invocation of none methods to Commands proxy - created by lettuce.
     * This allows us to support defaults methods in commands interface without relying much on
     * {@link java.lang.invoke.MethodHandles}
     * which is substituted here
     * {@link io.quarkus.redis.runtime.graal.DefaultMethodInvocationInterceptorSubstitute#invoke(MethodInvocation)}
     *
     * Example:
     *
     * If we have this interface:
     *
     * interface RedisCommands extends Commands {
     * String get(String key);
     * }
     *
     * We'll generate this class:
     *
     * @Singleton
     *            class RedisCommandsImpl implements RedisCommands {
     *
     *            RedisCommands command;
     *
     *            RedisCommandsImpl() {
     *            command = Arc.container().instance(QuarkusRedisCommandFactory.class).get().create(RedisCommands.class)
     *            }
     *
     *            public String get(String key) {
     *            return this.command.get(key);
     *            }
     *            }
     */
    void generate(ClassOutput classOutput, ClassInfo commandClassInfo) {
        String commandInterfaceName = commandClassInfo.name().toString();
        String packageName = commandInterfaceName.substring(0, commandInterfaceName.lastIndexOf(".") + 1);
        String className = packageName + HashUtil.sha1(commandClassInfo.name().toString()) + commandClassInfo.simpleName()
                + "Impl";
        try (ClassCreator commandClassCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .interfaces(commandInterfaceName)
                .className(className)
                .build()) {

            // a proxy instance
            FieldDescriptor commandFiled = commandClassCreator.getFieldCreator("command", commandInterfaceName)
                    .setModifiers(Modifier.PRIVATE)
                    .getFieldDescriptor();

            commandClassCreator.addAnnotation(Singleton.class.getName());
            try (MethodCreator ctor = commandClassCreator.getMethodCreator("<init>", void.class)) {
                ctor.setModifiers(Modifier.PUBLIC);
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ResultHandle self = ctor.getThis();

                // retrieve QuarkusRedisCommandFactory bean
                // QuarkusRedisCommandFactory factoryInstance = Arc.container().instance(QuarkusRedisCommandFactory.class).get()
                ResultHandle container = ctor
                        .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
                ResultHandle instanceHandle = ctor.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                                Annotation[].class),
                        container, ctor.loadClass(QuarkusRedisCommandFactory.class), ctor.loadNull());
                ResultHandle factoryInstance = ctor.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class), instanceHandle);

                // create Commands
                // command = factoryInstance.create(CommandInterface.class)
                ResultHandle interfaceHandle = ctor.loadClass(commandInterfaceName);
                ResultHandle command = ctor.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(QuarkusRedisCommandFactory.class, "create", Commands.class, Class.class),
                        factoryInstance, interfaceHandle);
                ctor.writeInstanceField(commandFiled, self, command);
                ctor.returnValue(null);
            }

            for (MethodInfo method : commandClassInfo.methods()) {
                if (isDefault(method.flags())) { // skip default methods
                    continue;
                }

                // delegate method invocation to proxy instance
                MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
                try (MethodCreator methodCreator = commandClassCreator.getMethodCreator(methodDescriptor)) {
                    ResultHandle command = methodCreator.readInstanceField(commandFiled, methodCreator.getThis());
                    int parameterSize = method.parameters().size();
                    ResultHandle[] resultHandles = new ResultHandle[parameterSize];

                    for (int i = 0; i < parameterSize; i++) {
                        resultHandles[i] = methodCreator.getMethodParam(i);
                    }

                    ResultHandle result = methodCreator.invokeInterfaceMethod(methodDescriptor, command, resultHandles);
                    methodCreator.returnValue(result);
                }

            }

        }
    }

    private boolean isDefault(short flags) {
        return ((flags & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC);
    }
}

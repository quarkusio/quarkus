package io.quarkus.hibernate.accessor.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.accessor.AccessorFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.hibernate.accessor.runtime.AccessorFactoryBootstrap;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorRecorder;
import io.quarkus.hibernate.accessor.runtime.HibernateAccessorStrategy;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;
import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.Opcode;
import io.smallrye.classfile.instruction.InvokeInstruction;

class HibernateAccessorBootstrapTest {
    private static final String BOOTSTRAP = HibernateAccessorGenerator.GENERATED_BOOTSTRAP;
    private static final String FACTORY = HibernateAccessorGenerator.QUARKUS_HIBERNATE_ACCESSOR_FACTORY;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void bootstrapDirectlyCallsFactoryWithoutContextClassLoader(boolean withFallback) throws Exception {
        Map<String, byte[]> classes = generate(withFallback);
        var bootstrapClass = ClassFile.of().parse(classes.get(BOOTSTRAP));
        assertThat(bootstrapClass.flags().flagsMask() & ClassFile.ACC_PUBLIC).isNotZero();
        assertThat(bootstrapClass.methods()).anySatisfy(method -> {
            assertThat(method.methodName().stringValue()).isEqualTo("<init>");
            assertThat(method.methodType().stringValue()).isEqualTo("()V");
            assertThat(method.flags().flagsMask() & ClassFile.ACC_PUBLIC).isNotZero();
        });
        List<InvokeInstruction> calls = calls(classes.get(BOOTSTRAP));
        assertThat(calls).anySatisfy(call -> {
            assertThat(call.opcode()).isEqualTo(Opcode.INVOKESTATIC);
            assertThat(call.owner().asInternalName()).isEqualTo(FACTORY.replace('.', '/'));
            assertThat(call.name().stringValue()).isEqualTo("create");
            assertThat(call.type().stringValue()).isEqualTo(withFallback
                    ? "(Lorg/hibernate/accessor/AccessorFactory;)Lorg/hibernate/accessor/AccessorFactory;"
                    : "()Lorg/hibernate/accessor/AccessorFactory;");
        });
        ClassLoader application = loader(classes);
        var bootstrap = (AccessorFactoryBootstrap) application.loadClass(BOOTSTRAP).getConstructor().newInstance();
        assertThat(bootstrap.getClass().getClassLoader()).isSameAs(application);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new ClassLoader(null) {
            });
            var recorder = new HibernateAccessorRecorder();
            AccessorFactory factory = (withFallback
                    ? recorder.createAccessorFactoryWithFallback(new RuntimeValue<>(bootstrap))
                    : recorder.createAccessorFactory(new RuntimeValue<>(bootstrap))).getValue();
            checkFactory(factory, withFallback);
            // Later calls through the bootstrap must retain the first initialized factory and fallback.
            assertThat(bootstrap.create(recorder.createReflectionFactory().getValue())).isSameAs(factory);
            assertThat(bootstrap.create(null)).isSameAs(factory);
            checkFactory(factory, withFallback);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    @ParameterizedTest
    @EnumSource(HibernateAccessorStrategy.class)
    void startupConstructsBootstrapDirectlyAndPublishesFactory(HibernateAccessorStrategy strategy) throws Exception {
        Map<String, byte[]> classes = strategy == HibernateAccessorStrategy.REFLECTION
                ? new LinkedHashMap<>()
                : generate(strategy == HibernateAccessorStrategy.GENERATED_WITH_FALLBACK);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (var recordingLoader = QuarkusClassLoader.builder("accessor recording", getClass().getClassLoader(), true)
                .build()) {
            Thread.currentThread().setContextClassLoader(recordingLoader);
            var recording = new BytecodeRecorderImpl(true, "HibernateAccessorBootstrapTest", "accessFactory", strategy.name(),
                    true);
            var item = new HibernateAccessorProcessor().accessFactory(() -> strategy,
                    recording.getRecordingProxy(HibernateAccessorRecorder.class), recording);
            Map<String, byte[]> startup = new LinkedHashMap<>();
            recording.writeBytecode(startup::put);
            List<InvokeInstruction> calls = startup.values().stream().flatMap(bytes -> calls(bytes).stream()).toList();
            List<InvokeInstruction> bootstrapCalls = calls.stream()
                    .filter(call -> call.owner().asInternalName().equals(BOOTSTRAP.replace('.', '/'))).toList();
            if (strategy == HibernateAccessorStrategy.REFLECTION) {
                assertThat(bootstrapCalls).isEmpty();
            } else {
                assertThat(bootstrapCalls).singleElement().satisfies(call -> {
                    assertThat(call.opcode()).isEqualTo(Opcode.INVOKESPECIAL);
                    assertThat(call.name().stringValue()).isEqualTo("<init>");
                    assertThat(call.type().stringValue()).isEqualTo("()V");
                });
            }
            startup.forEach((name, bytes) -> classes.put(name.replace('/', '.'), bytes));
            var task = (StartupTask) loader(classes).loadClass(recording.getClassName()).getConstructor().newInstance();
            try (var context = new StartupContext()) {
                task.deploy(context);
                var returned = (BytecodeRecorderImpl.ReturnedProxy) item.accessorFactory();
                var factory = (RuntimeValue<?>) context.getValue(returned.__returned$proxy$key());
                checkFactory((AccessorFactory) factory.getValue(), strategy != HibernateAccessorStrategy.GENERATED);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static void checkFactory(AccessorFactory factory, boolean withFallback) throws Exception {
        var field = Unregistered.class.getDeclaredField("value");
        if (withFallback) {
            assertThat(factory.valueReader(field).get(new Unregistered())).isEqualTo("fallback");
        } else {
            assertThatThrownBy(() -> factory.valueReader(field)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private static Map<String, byte[]> generate(boolean withFallback) {
        Map<String, byte[]> classes = new LinkedHashMap<>(
                HibernateAccessorGenerator.generator().generateClasses(List.of(), withFallback));
        List<GeneratedClassBuildItem> output = new ArrayList<>();
        HibernateAccessorGenerator.generateBootstrap(output::add, withFallback);
        assertThat(output).singleElement().satisfies(item -> {
            assertThat(item.isApplicationClass()).isTrue();
            assertThat(item.binaryName()).isEqualTo(BOOTSTRAP);
            classes.put(item.binaryName(), item.getClassData());
        });
        return classes;
    }

    private static List<InvokeInstruction> calls(byte[] bytes) {
        return ClassFile.of().parse(bytes).methods().stream().flatMap(method -> method.code().stream())
                .flatMap(code -> code.elementList().stream()).filter(InvokeInstruction.class::isInstance)
                .map(InvokeInstruction.class::cast).toList();
    }

    private static ClassLoader loader(Map<String, byte[]> classes) {
        return new ClassLoader(HibernateAccessorBootstrapTest.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name);
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }

    static class Unregistered {
        private String value = "fallback";
    }
}

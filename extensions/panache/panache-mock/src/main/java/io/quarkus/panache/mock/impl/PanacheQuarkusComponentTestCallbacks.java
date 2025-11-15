package io.quarkus.panache.mock.impl;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.processor.BytecodeTransformer;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.panache.common.deployment.PanacheConstants;
import io.quarkus.panache.mock.MockPanacheEntities;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.component.QuarkusComponentTestCallbacks;

public class PanacheQuarkusComponentTestCallbacks implements QuarkusComponentTestCallbacks {

    private static final DotName PANACHE_ENTITY = DotName.createSimple("io.quarkus.hibernate.orm.panache.PanacheEntity");
    private static final DotName PANACHE_ENTITY_BASE = DotName
            .createSimple("io.quarkus.hibernate.orm.panache.PanacheEntityBase");

    @Override
    public void beforeBuild(BeforeBuildContext buildContext) {
        Class<?> testClass = buildContext.getTestClass();
        MockPanacheEntities panacheMocks = testClass.getAnnotation(MockPanacheEntities.class);
        if (panacheMocks == null || panacheMocks.value().length == 0) {
            return;
        }
        List<ClassInfo> mockedEntities = new ArrayList<>();
        for (Class<?> mockClass : panacheMocks.value()) {
            ClassInfo maybeMockedEntity = buildContext.getComputingBeanArchiveIndex().getClassByName(mockClass);
            DotName superName = maybeMockedEntity.superName();
            if (maybeMockedEntity.isAbstract()
                    || superName == null
                    || (!superName.equals(PANACHE_ENTITY)
                            && !superName.equals(PANACHE_ENTITY_BASE))) {
                continue;
            }
            mockedEntities.add(maybeMockedEntity);
        }

        Map<DotName, List<MethodInfo>> entityUserMethods = new HashMap<>();
        for (ClassInfo entity : mockedEntities) {
            for (MethodInfo method : entity.methods()) {
                if (!method.isSynthetic()
                        && Modifier.isStatic(method.flags())
                        && Modifier.isPublic(method.flags())) {
                    List<MethodInfo> userMethods = entityUserMethods.get(entity.name());
                    if (userMethods == null) {
                        userMethods = new ArrayList<>();
                        entityUserMethods.put(entity.name(), userMethods);
                    }
                    userMethods.add(method);
                }
            }
        }

        Map<DotName, List<MethodInfo>> entityGeneratedMethods = new HashMap<>();
        ClassInfo panacheEntityBaseClassInfo = buildContext.getComputingBeanArchiveIndex()
                .getClassByName(DotName.createSimple("io.quarkus.hibernate.orm.panache.PanacheEntityBase"));
        for (ClassInfo entity : mockedEntities) {
            for (MethodInfo method : panacheEntityBaseClassInfo.methods()) {
                if (!userMethodExists(entityUserMethods.get(entity.name()), method)) {
                    AnnotationInstance bridge = method.annotation(PanacheConstants.DOTNAME_GENERATE_BRIDGE);
                    if (bridge != null) {
                        // TODO bridge.value("targetReturnTypeErased") and bridge.value("callSuperMethod")
                        List<MethodInfo> generated = entityGeneratedMethods.get(entity.name());
                        if (generated == null) {
                            generated = new ArrayList<>();
                            entityGeneratedMethods.put(entity.name(), generated);
                        }
                        generated.add(method);
                    }
                }
            }
        }

        for (ClassInfo entity : mockedEntities) {
            String entityClassName = entity.name().toString();
            ClassTransformer transformer = new ClassTransformer(entity.name().toString());
            List<MethodInfo> userMethods = entityUserMethods.get(entity.name());
            if (userMethods != null) {
                for (MethodInfo userMethod : userMethods) {
                    transformer.removeMethod(MethodDescriptor.of(userMethod));
                    addMethod(entityClassName, transformer, userMethod);
                }
            }
            List<MethodInfo> generatedMethods = entityGeneratedMethods.get(entity.name());
            if (generatedMethods != null) {
                for (MethodInfo generatedMethod : generatedMethods) {
                    addMethod(entityClassName, transformer, generatedMethod);
                }
            }
            buildContext.addBytecodeTransformer(
                    new BytecodeTransformer(entityClassName, new BiFunction<String, ClassVisitor, ClassVisitor>() {
                        @Override
                        public ClassVisitor apply(String className, ClassVisitor originalVisitor) {
                            return transformer.applyTo(originalVisitor);
                        }
                    }));
        }
    }

    private void addMethod(String entityClass, ClassTransformer transformer, MethodInfo method) {
        MethodCreator transform = transformer.addMethod(MethodDescriptor.of(method));
        transform.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        // if (!PanacheMock.IsMockEnabled)
        //   throw new RuntimeException("Panache mock not enabled");
        // if (!PanacheMock.isMocked(TestClass.class))
        //   throw new RuntimeException("FooEntity not mocked");
        // try {
        //    return (int)PanacheMock.mockMethod(TestClass.class, "foo", new Class<?>[] {int.class}, new Object[] {arg});
        // } catch (PanacheMock.InvokeRealMethodException e) {
        //    throw new RuntimeException("Unstubbed method called", e);
        // }
        ResultHandle isMockEnabled = transform
                .readStaticField(FieldDescriptor.of(PanacheMock.class, "IsMockEnabled", boolean.class));
        BytecodeCreator mockNotEnabled = transform.ifTrue(isMockEnabled).falseBranch();
        mockNotEnabled.throwException(RuntimeException.class, "Panache mock not enabled");
        ResultHandle isMocked = transform.invokeStaticMethod(
                MethodDescriptor.ofMethod(PanacheMock.class, "isMocked", boolean.class, Class.class),
                transform.loadClass(entityClass));
        BytecodeCreator notMocked = transform.ifTrue(isMocked).falseBranch();
        notMocked.throwException(RuntimeException.class, entityClass + " not mocked");
        ResultHandle entityClazz = transform.loadClass(entityClass);
        ResultHandle methodName = transform.load(method.name());
        ResultHandle paramTypes = transform.newArray(Class.class, method.parametersCount());
        for (int i = 0; i < method.parametersCount(); i++) {
            transform.writeArrayValue(paramTypes, i, transform.loadClass(method.parameterType(i).name().toString()));
        }
        ResultHandle args = transform.newArray(Object.class, method.parametersCount());
        for (int i = 0; i < method.parametersCount(); i++) {
            transform.writeArrayValue(args, i, transform.getMethodParam(i));
        }
        TryBlock tryInvoke = transform.tryBlock();
        ResultHandle ret = tryInvoke.invokeStaticMethod(MethodDescriptor.ofMethod(PanacheMock.class, "mockMethod",
                Object.class, Class.class, String.class, Class[].class, Object[].class), entityClazz, methodName,
                paramTypes,
                args);
        tryInvoke.returnValue(ret);
        CatchBlockCreator catched = tryInvoke.addCatch(PanacheMock.InvokeRealMethodException.class);
        catched.throwException(RuntimeException.class,
                "Unstubbed method called: " + method.toString(), catched.getCaughtException());
    }

    @Override
    public void afterStart(AfterStartContext afterStartContext) {
        Class<?> testClass = afterStartContext.getTestClass();
        MockPanacheEntities panacheMocks = testClass.getAnnotation(MockPanacheEntities.class);
        if (panacheMocks == null || panacheMocks.value().length == 0) {
            return;
        }
        PanacheMock.mock(panacheMocks.value());
    }

    @Override
    public void afterStop(AfterStopContext afterStopContext) {
        PanacheMock.reset();
    }

    private boolean userMethodExists(List<MethodInfo> userMethods, MethodInfo method) {
        if (userMethods == null || userMethods.isEmpty()) {
            return false;
        }
        String descriptor = method.descriptor();
        for (MethodInfo userMethod : userMethods) {
            if (userMethod.descriptor().equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

}

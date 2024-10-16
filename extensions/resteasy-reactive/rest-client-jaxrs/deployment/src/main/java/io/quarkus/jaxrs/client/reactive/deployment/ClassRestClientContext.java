package io.quarkus.jaxrs.client.reactive.deployment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.jaxrs.client.reactive.runtime.ParameterAnnotationsSupplier;
import io.quarkus.jaxrs.client.reactive.runtime.ParameterDescriptorFromClassSupplier;
import io.quarkus.jaxrs.client.reactive.runtime.ParameterGenericTypesSupplier;

class ClassRestClientContext implements AutoCloseable {

    public final ClassCreator classCreator;
    public final MethodCreator constructor;
    public final MethodCreator clinit;

    public final Map<Integer, FieldDescriptor> methodStaticFields = new HashMap<>();
    public final Map<Integer, FieldDescriptor> methodParamAnnotationsStaticFields = new HashMap<>();
    public final Map<Integer, FieldDescriptor> methodGenericParametersStaticFields = new HashMap<>();
    public final Map<String, FieldDescriptor> beanTypesParameterDescriptorsStaticFields = new HashMap<>();
    public final Map<String, ResultHandle> classesMap = new HashMap<>();
    private int beanParamIndex = 0;

    public ClassRestClientContext(String name, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            String... interfaces) {
        this(name, MethodDescriptor.ofConstructor(name), generatedClasses, Object.class, interfaces);
    }

    public ClassRestClientContext(String name, MethodDescriptor constructorDesc,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            Class<?> superClass, String... interfaces) {

        this.classCreator = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClasses, true),
                name, null, superClass.getName(), interfaces);
        this.constructor = classCreator.getMethodCreator(constructorDesc);
        this.clinit = classCreator.getMethodCreator(MethodDescriptor.ofMethod(name, "<clinit>", void.class));
        this.clinit.setModifiers(Opcodes.ACC_STATIC);
    }

    @Override
    public void close() {
        classCreator.close();
    }

    protected FieldDescriptor createJavaMethodField(ClassInfo interfaceClass, MethodInfo method, int methodIndex) {
        ResultHandle interfaceClassHandle = loadClass(interfaceClass.toString());

        ResultHandle parameterArray = clinit.newArray(Class.class, method.parametersCount());
        for (int i = 0; i < method.parametersCount(); i++) {
            String parameterClass = method.parameterType(i).name().toString();
            clinit.writeArrayValue(parameterArray, i, loadClass(parameterClass));
        }

        ResultHandle javaMethodHandle = clinit.invokeVirtualMethod(
                MethodDescriptor.ofMethod(Class.class, "getMethod", Method.class, String.class, Class[].class),
                interfaceClassHandle, clinit.load(method.name()), parameterArray);
        FieldDescriptor javaMethodField = FieldDescriptor.of(classCreator.getClassName(), "javaMethod" + methodIndex,
                Method.class);
        classCreator.getFieldCreator(javaMethodField).setModifiers(Modifier.PRIVATE | Modifier.FINAL | Modifier.STATIC);
        clinit.writeStaticField(javaMethodField, javaMethodHandle);

        methodStaticFields.put(methodIndex, javaMethodField);

        return javaMethodField;
    }

    /**
     * Generates "method.getParameterAnnotations()" and it will only be created if and only if the supplier is used
     * in order to not have a penalty performance.
     */
    protected Supplier<FieldDescriptor> getLazyJavaMethodParamAnnotationsField(int methodIndex) {
        return () -> {
            FieldDescriptor methodParamAnnotationsField = methodParamAnnotationsStaticFields.get(methodIndex);
            if (methodParamAnnotationsField != null) {
                return methodParamAnnotationsField;
            }

            ResultHandle javaMethodParamAnnotationsHandle = clinit.newInstance(MethodDescriptor.ofConstructor(
                    ParameterAnnotationsSupplier.class, Method.class),
                    clinit.readStaticField(methodStaticFields.get(methodIndex)));
            FieldDescriptor javaMethodParamAnnotationsField = FieldDescriptor.of(classCreator.getClassName(),
                    "javaMethodParameterAnnotations" + methodIndex, Supplier.class);
            classCreator.getFieldCreator(javaMethodParamAnnotationsField)
                    .setModifiers(Modifier.FINAL | Modifier.STATIC); // needs to be package-private because it's used by subresources
            clinit.writeStaticField(javaMethodParamAnnotationsField, javaMethodParamAnnotationsHandle);

            methodParamAnnotationsStaticFields.put(methodIndex, javaMethodParamAnnotationsField);

            return javaMethodParamAnnotationsField;
        };
    }

    /**
     * Generates "method.getGenericParameterTypes()" and it will only be created if and only if the supplier is used
     * in order to not have a penalty performance.
     */
    protected Supplier<FieldDescriptor> getLazyJavaMethodGenericParametersField(int methodIndex) {
        return () -> {
            FieldDescriptor methodGenericTypeField = methodGenericParametersStaticFields.get(methodIndex);
            if (methodGenericTypeField != null) {
                return methodGenericTypeField;
            }

            ResultHandle javaMethodGenericParametersHandle = clinit.newInstance(MethodDescriptor.ofConstructor(
                    ParameterGenericTypesSupplier.class, Method.class),
                    clinit.readStaticField(methodStaticFields.get(methodIndex)));
            FieldDescriptor javaMethodGenericParametersField = FieldDescriptor.of(classCreator.getClassName(),
                    "javaMethodGenericParameters" + methodIndex, Supplier.class);
            classCreator.getFieldCreator(javaMethodGenericParametersField)
                    .setModifiers(Modifier.FINAL | Modifier.STATIC); // needs to be package-private because it's used by subresources
            clinit.writeStaticField(javaMethodGenericParametersField, javaMethodGenericParametersHandle);

            methodGenericParametersStaticFields.put(methodIndex, javaMethodGenericParametersField);

            return javaMethodGenericParametersField;
        };
    }

    /**
     * Generates "Class.forName(beanClass)" to generate the parameter descriptors. This method will only be created if and only
     * if the supplier is used in order to not have a penalty performance.
     */
    protected Supplier<FieldDescriptor> getLazyBeanParameterDescriptors(String beanClass) {
        return () -> {
            FieldDescriptor field = beanTypesParameterDescriptorsStaticFields.get(beanClass);
            if (field != null) {
                return field;
            }

            ResultHandle clazz = loadClass(beanClass);

            ResultHandle mapWithAnnotationsHandle = clinit.newInstance(MethodDescriptor.ofConstructor(
                    ParameterDescriptorFromClassSupplier.class, Class.class),
                    clazz);
            field = FieldDescriptor.of(classCreator.getClassName(), "beanParamDescriptors" + beanParamIndex, Supplier.class);
            classCreator.getFieldCreator(field).setModifiers(Modifier.FINAL | Modifier.STATIC); // needs to be package-private because it's used by subresources
            clinit.writeStaticField(field, mapWithAnnotationsHandle);

            beanTypesParameterDescriptorsStaticFields.put(beanClass, field);

            beanParamIndex++;

            return field;
        };
    }

    private ResultHandle loadClass(String className) {
        ResultHandle classType = classesMap.get(className);
        if (classType != null) {
            return classType;
        }

        ResultHandle classFromTCCL = clinit.loadClassFromTCCL(className);
        classesMap.put(className, classFromTCCL);
        return classFromTCCL;
    }
}

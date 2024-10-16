package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

final class SyntheticComponentsUtil {
    private static final String FIELD_NAME_PARAMS = "params";

    /**
     * Adds the {@code params} field to given class and adds constructor code to initialize the field.
     *
     * @param classCreator class to which the {@code params} field will be added
     * @param constructor constructor in which the {@code params} field will be initialized
     * @param params the parameter map, will be "copied" to the {@code params} field
     * @param annotationLiterals to allow creating annotation literals
     * @param beanArchiveIndex to find annotation types when generating annotation literal classes
     */
    static void addParamsFieldAndInit(ClassCreator classCreator, MethodCreator constructor, Map<String, Object> params,
            AnnotationLiteralProcessor annotationLiterals, IndexView beanArchiveIndex) {

        FieldCreator field = classCreator.getFieldCreator(FIELD_NAME_PARAMS, Map.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        ResultHandle paramsHandle;
        if (params.isEmpty()) {
            paramsHandle = constructor.invokeStaticMethod(MethodDescriptors.COLLECTIONS_EMPTY_MAP);
        } else {
            paramsHandle = constructor.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                ResultHandle valHandle = null;
                if (entry.getValue() instanceof Boolean) {
                    valHandle = constructor.load(((Boolean) entry.getValue()).booleanValue());
                } else if (entry.getValue() instanceof boolean[]) {
                    boolean[] array = (boolean[]) entry.getValue();
                    valHandle = constructor.newArray(boolean.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Byte) {
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Byte.class, byte.class),
                            constructor.load(((Byte) entry.getValue()).byteValue()));
                } else if (entry.getValue() instanceof byte[]) {
                    byte[] array = (byte[]) entry.getValue();
                    valHandle = constructor.newArray(byte.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Short) {
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Short.class, short.class),
                            constructor.load(((Short) entry.getValue()).shortValue()));
                } else if (entry.getValue() instanceof short[]) {
                    short[] array = (short[]) entry.getValue();
                    valHandle = constructor.newArray(short.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Integer) {
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Integer.class, int.class),
                            constructor.load(((Integer) entry.getValue()).intValue()));
                } else if (entry.getValue() instanceof int[]) {
                    int[] array = (int[]) entry.getValue();
                    valHandle = constructor.newArray(int.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Long) {
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Long.class, long.class),
                            constructor.load(((Long) entry.getValue()).longValue()));
                } else if (entry.getValue() instanceof long[]) {
                    long[] array = (long[]) entry.getValue();
                    valHandle = constructor.newArray(long.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Float) {
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Float.class, float.class),
                            constructor.load(((Float) entry.getValue()).floatValue()));
                } else if (entry.getValue() instanceof float[]) {
                    float[] array = (float[]) entry.getValue();
                    valHandle = constructor.newArray(float.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Double) {
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Double.class, double.class),
                            constructor.load(((Double) entry.getValue()).doubleValue()));
                } else if (entry.getValue() instanceof double[]) {
                    double[] array = (double[]) entry.getValue();
                    valHandle = constructor.newArray(double.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Character) {
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(Character.class, char.class),
                            constructor.load(((Character) entry.getValue()).charValue()));
                } else if (entry.getValue() instanceof char[]) {
                    char[] array = (char[]) entry.getValue();
                    valHandle = constructor.newArray(char.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof String) {
                    valHandle = constructor.load((String) entry.getValue());
                } else if (entry.getValue() instanceof String[]) {
                    String[] array = (String[]) entry.getValue();
                    valHandle = constructor.newArray(String.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Enum) {
                    valHandle = constructor.load((Enum<?>) entry.getValue());
                } else if (entry.getValue() instanceof Enum[]) {
                    Enum<?>[] array = (Enum<?>[]) entry.getValue();
                    // most commonly, all values in the array are of the same type, in which case we create an array
                    // of that type; otherwise, we create an array of Enum, which is the least upper bound
                    Class<?> arrayElementClass = array.length == 0 ? Enum.class : array[0].getDeclaringClass();
                    for (Enum<?> value : array) {
                        if (!arrayElementClass.equals(value.getDeclaringClass())) {
                            arrayElementClass = Enum.class;
                            break;
                        }
                    }
                    valHandle = constructor.newArray(arrayElementClass, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.load(array[i]));
                    }
                } else if (entry.getValue() instanceof Class) {
                    valHandle = constructor.loadClassFromTCCL((Class<?>) entry.getValue());
                } else if (entry.getValue() instanceof Class[]) {
                    Class<?>[] array = (Class<?>[]) entry.getValue();
                    valHandle = constructor.newArray(Class.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.loadClassFromTCCL(array[i]));
                    }
                } else if (entry.getValue() instanceof ClassInfo) {
                    valHandle = constructor.loadClassFromTCCL(((ClassInfo) entry.getValue()).name().toString());
                } else if (entry.getValue() instanceof ClassInfo[]) {
                    ClassInfo[] array = (ClassInfo[]) entry.getValue();
                    valHandle = constructor.newArray(Class.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i, constructor.loadClassFromTCCL(array[i].name().toString()));
                    }
                } else if (entry.getValue() instanceof AnnotationInstance) {
                    AnnotationInstance annotationInstance = (AnnotationInstance) entry.getValue();
                    ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotationInstance.name());
                    valHandle = annotationLiterals.create(constructor, annotationClass, annotationInstance);
                } else if (entry.getValue() instanceof AnnotationInstance[]) {
                    AnnotationInstance[] array = (AnnotationInstance[]) entry.getValue();
                    // most commonly, all values in the array are of the same type, in which case we create an array
                    // of that type; otherwise, we create an array of Annotation, which is the least upper bound
                    String arrayElementClass = array.length == 0 ? Annotation.class.getName() : array[0].name().toString();
                    for (AnnotationInstance value : array) {
                        if (!arrayElementClass.equals(value.name().toString())) {
                            arrayElementClass = Annotation.class.getName();
                            break;
                        }
                    }
                    valHandle = constructor.newArray(arrayElementClass, array.length);
                    for (int i = 0; i < array.length; i++) {
                        AnnotationInstance annotationInstance = array[i];
                        ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotationInstance.name());
                        ResultHandle elementHandle = annotationLiterals.create(constructor, annotationClass,
                                annotationInstance);
                        constructor.writeArrayValue(valHandle, i, elementHandle);
                    }
                } else if (entry.getValue() instanceof InvokerInfo) {
                    InvokerInfo invoker = (InvokerInfo) entry.getValue();
                    valHandle = constructor.newInstance(MethodDescriptor.ofConstructor(invoker.getClassName()));
                } else if (entry.getValue() instanceof InvokerInfo[]) {
                    InvokerInfo[] array = (InvokerInfo[]) entry.getValue();
                    valHandle = constructor.newArray(Invoker.class, array.length);
                    for (int i = 0; i < array.length; i++) {
                        constructor.writeArrayValue(valHandle, i,
                                constructor.newInstance(MethodDescriptor.ofConstructor(array[i].getClassName())));
                    }
                }

                constructor.invokeInterfaceMethod(MethodDescriptors.MAP_PUT, paramsHandle,
                        constructor.load(entry.getKey()), valHandle);
            }
        }

        constructor.writeInstanceField(field.getFieldDescriptor(), constructor.getThis(), paramsHandle);
    }
}

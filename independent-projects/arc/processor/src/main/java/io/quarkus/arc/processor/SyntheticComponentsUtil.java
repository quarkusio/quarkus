package io.quarkus.arc.processor;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.Reflection2Gizmo;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

final class SyntheticComponentsUtil {
    private static final String FIELD_NAME_PARAMS = "params";

    /**
     * Adds the {@code params} field to given class and adds constructor code to initialize the field.
     *
     * @param cc class to which the {@code params} field will be added
     * @param bc constructor in which the {@code params} field will be initialized
     * @param params the parameter map, will be "copied" to the {@code params} field
     * @param annotationLiterals to allow creating annotation literals
     * @param beanArchiveIndex to find annotation types when generating annotation literal classes
     */
    static void addParamsFieldAndInit(io.quarkus.gizmo2.creator.ClassCreator cc, BlockCreator bc,
            Map<String, Object> params, AnnotationLiteralProcessor annotationLiterals, IndexView beanArchiveIndex) {

        FieldDesc paramsField = cc.field(FIELD_NAME_PARAMS, fc -> {
            fc.private_();
            fc.final_();
            fc.setType(Map.class);
        });

        LocalVar tccl = bc.localVar("tccl", bc.invokeVirtual(MethodDescs.THREAD_GET_TCCL, bc.currentThread()));

        LocalVar paramsVar;
        if (params.isEmpty()) {
            paramsVar = bc.localVar("params", bc.mapOf());
        } else if (params.size() <= 10) {
            List<Expr> paramsMapContent = new ArrayList<>(params.size() * 2);
            for (Entry<String, Object> paramEntry : params.entrySet()) {
                paramsMapContent.add(Const.of(paramEntry.getKey()));
                paramsMapContent.add(convertParamValue(paramEntry.getValue(), tccl, bc, annotationLiterals, beanArchiveIndex));
            }
            paramsVar = bc.localVar("params", bc.mapOf(paramsMapContent));
        } else {
            int initialCapacity = (int) (params.size() / 0.75f + 1.0f);
            paramsVar = bc.localVar("params", bc.new_(HashMap.class, Const.of(initialCapacity)));
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                LocalVar value = convertParamValue(entry.getValue(), tccl, bc, annotationLiterals, beanArchiveIndex);
                bc.withMap(paramsVar).put(Const.of(entry.getKey()), value);
            }
        }

        bc.set(cc.this_().field(paramsField), paramsVar);
    }

    private static LocalVar convertParamValue(Object paramValue, Var tccl, BlockCreator bc,
            AnnotationLiteralProcessor annotationLiterals, IndexView beanArchiveIndex) {
        LocalVar value;
        if (paramValue instanceof Boolean val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof boolean[] array) {
            value = bc.localVar("value", bc.newEmptyArray(boolean.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Byte val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof byte[] array) {
            value = bc.localVar("value", bc.newEmptyArray(byte.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Short val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof short[] array) {
            value = bc.localVar("value", bc.newEmptyArray(short.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Integer val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof int[] array) {
            value = bc.localVar("value", bc.newEmptyArray(int.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Long val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof long[] array) {
            value = bc.localVar("value", bc.newEmptyArray(long.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Float val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof float[] array) {
            value = bc.localVar("value", bc.newEmptyArray(float.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Double val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof double[] array) {
            value = bc.localVar("value", bc.newEmptyArray(double.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Character val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof char[] array) {
            value = bc.localVar("value", bc.newEmptyArray(char.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof String val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof String[] array) {
            value = bc.localVar("value", bc.newEmptyArray(String.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Enum<?> val) {
            value = bc.localVar("value", Const.of(val));
        } else if (paramValue instanceof Enum<?>[] array) {
            // most commonly, all values in the array are of the same type, in which case we create an array
            // of that type; otherwise, we create an array of Enum, which is the least upper bound
            Class<?> arrayElementClass = array.length == 0 ? Enum.class : array[0].getDeclaringClass();
            for (Enum<?> enumVal : array) {
                if (!arrayElementClass.equals(enumVal.getDeclaringClass())) {
                    arrayElementClass = Enum.class;
                    break;
                }
            }
            value = bc.localVar("value", bc.newEmptyArray(arrayElementClass, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), Const.of(array[i]));
            }
        } else if (paramValue instanceof Class<?> val) {
            value = bc.localVar("value", loadClassFromTCCL(val, tccl, bc));
        } else if (paramValue instanceof Class<?>[] array) {
            value = bc.localVar("value", bc.newEmptyArray(Class.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), loadClassFromTCCL(array[i], tccl, bc));
            }
        } else if (paramValue instanceof ClassInfo val) {
            value = bc.localVar("value", loadClassFromTCCL(classDescOf(val), tccl, bc));
        } else if (paramValue instanceof ClassInfo[] array) {
            value = bc.localVar("value", bc.newEmptyArray(Class.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), loadClassFromTCCL(classDescOf(array[i]), tccl, bc));
            }
        } else if (paramValue instanceof AnnotationInstance val) {
            ClassInfo annotationClass = beanArchiveIndex.getClassByName(val.name());
            value = bc.localVar("value", annotationLiterals.create(bc, annotationClass, val));
        } else if (paramValue instanceof AnnotationInstance[] array) {
            // most commonly, all values in the array are of the same type, in which case we create an array
            // of that type; otherwise, we create an array of Annotation, which is the least upper bound
            ClassDesc arrayElementClass = array.length == 0
                    ? ClassDesc.of(Annotation.class.getName())
                    : classDescOf(array[0].name());
            for (AnnotationInstance annVal : array) {
                if (!arrayElementClass.equals(classDescOf(annVal.name()))) {
                    arrayElementClass = ClassDesc.of(Annotation.class.getName());
                    break;
                }
            }

            value = bc.localVar("value", bc.newEmptyArray(arrayElementClass, array.length));
            for (int i = 0; i < array.length; i++) {
                AnnotationInstance annotationInstance = array[i];
                ClassInfo annotationClass = beanArchiveIndex.getClassByName(annotationInstance.name());
                bc.set(value.elem(i), annotationLiterals.create(bc, annotationClass, annotationInstance));
            }
        } else if (paramValue instanceof InvokerInfo val) {
            value = bc.localVar("value", bc.new_(val.getClassDesc()));
        } else if (paramValue instanceof InvokerInfo[] array) {
            value = bc.localVar("value", bc.newEmptyArray(Invoker.class, array.length));
            for (int i = 0; i < array.length; i++) {
                bc.set(value.elem(i), bc.new_(array[i].getClassDesc()));
            }
        } else {
            throw new IllegalArgumentException("Unsupported parameter type: " + paramValue);
        }
        return value;
    }

    private static Expr loadClassFromTCCL(Class<?> clazz, Var tccl, BlockCreator bc) {
        return loadClassFromTCCL(Reflection2Gizmo.classDescOf(clazz), tccl, bc);
    }

    private static Expr loadClassFromTCCL(ClassDesc clazz, Var tccl, BlockCreator bc) {
        if (clazz.isPrimitive()) {
            return Const.of(clazz);
        }

        String desc = clazz.descriptorString();
        String className = desc.substring(1, desc.length() - 1).replace('/', '.');

        if (className.startsWith("java.")) {
            return Const.of(clazz);
        }

        return bc.invokeStatic(
                MethodDesc.of(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                Const.of(className), Const.of(false), tccl);
    }
}

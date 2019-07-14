package io.quarkus.resteasy.jsonb.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

public class SerializationClassInspector {

    private final Map<DotName, Result> classInspectionResultMap = new HashMap<>();

    private final IndexView index;

    public SerializationClassInspector(IndexView index) {
        this.index = index;
    }

    public Result inspect(DotName classDotName) {
        ClassInfo classInfo = index.getClassByName(classDotName);
        if (classInfo == null) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        if (!Modifier.isPublic(classInfo.flags())) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        if (Modifier.isInterface(classInfo.flags()) || !index.getAllKnownSubclasses(classDotName).isEmpty()) {
            // if the class is an interface or is subclassed  we ignore it because json-b
            // adds all the properties of the implementation or subclasses (which we can't know)
            // TODO investigate if we could relax these constraints by checking if there
            // there are no implementations or subclasses that contain properties other than those
            // of the interface or class
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        if (classInspectionResultMap.containsKey(classInfo.name())) {
            return classInspectionResultMap.get(classInfo.name());
        }

        if (!DotNames.OBJECT.equals(classInfo.superName())) {
            // for now don't handle classes with super types other than object, too many corner cases
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        if (JandexUtil.containsInterfacesWithDefaultMethods(classInfo, index)) {
            // for now don't handle default methods either
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        Map<DotName, AnnotationInstance> effectiveClassAnnotations = JandexUtil.getEffectiveClassAnnotations(classInfo.name(),
                index);

        if (effectiveClassAnnotations.containsKey(DotNames.JSONB_TYPE_SERIALIZER)) {
            // we don't need to do anything since the type already has a serializer
            return SerializationClassInspector.Result.notPossible(classInfo);
        }
        if (effectiveClassAnnotations.containsKey(DotNames.JSONB_TYPE_ADAPTER)) {
            // for now we don't handle adapters at all
            return SerializationClassInspector.Result.notPossible(classInfo);
        }
        if (effectiveClassAnnotations.containsKey(DotNames.JSONB_VISIBILITY)) {
            // for now we don't handle visibility
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        Map<MethodInfo, FieldInfo> getters = PropertyUtil.getGetterMethods(classInfo);
        if (removeTransientGetters(getters)) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }
        for (MethodInfo methodInfo : getters.keySet()) {
            // currently we don't support generating serializers that contains items of the same class
            if (hasReferenceToClassType(methodInfo.returnType(), classDotName)) {
                return Result.notPossible(classInfo);
            }
        }

        List<FieldInfo> fields = PropertyUtil.getPublicFieldsWithoutGetters(classInfo, getters.keySet());
        if (removeTransientFields(fields)) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }
        for (FieldInfo field : fields) {
            if (hasReferenceToClassType(field.type(), classDotName)) {
                return Result.notPossible(classInfo);
            }
        }

        SerializationClassInspector.Result result = SerializationClassInspector.Result.possible(classInfo,
                effectiveClassAnnotations, getters, fields);
        classInspectionResultMap.put(classInfo.name(), result);
        return result;
    }

    /**
     * Removes fields annotated with @JsonbTransient
     *
     * @return true if the serializer can't be generated
     */
    private boolean removeTransientFields(List<FieldInfo> fields) {
        // TODO handle @JsonbTransient meta-annotations

        Iterator<FieldInfo> fieldsIterator = fields.iterator();
        while (fieldsIterator.hasNext()) {
            FieldInfo next = fieldsIterator.next();

            if (next.hasAnnotation(DotNames.JSONB_TRANSIENT)) {
                int jsonbAnnotationCount = 0;
                for (AnnotationInstance annotation : next.annotations()) {
                    if (annotation.name().toString().contains("json.bind")) {
                        jsonbAnnotationCount++;
                    }
                }
                if (jsonbAnnotationCount > 1) {
                    // we bail out and let jsonb handle this case at runtime (which will end up throwing an exception)
                    return true;
                } else {
                    // the field was annotated with the @JsonbTransient so we ignore it
                    fieldsIterator.remove();
                }
            }
        }
        return false;
    }

    /**
     * Removes getters annotated with @JsonbTransient
     *
     * @return true if the serializer can't be generated
     */
    private boolean removeTransientGetters(Map<MethodInfo, FieldInfo> getters) {
        // TODO handle @JsonbTransient meta-annotations

        Iterator<Map.Entry<MethodInfo, FieldInfo>> iterator = getters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MethodInfo, FieldInfo> next = iterator.next();

            if (next.getKey().hasAnnotation(DotNames.JSONB_TRANSIENT)) {
                int jsonbAnnotationCount = 0;
                for (AnnotationInstance annotation : next.getKey().annotations()) {
                    if (annotation.target().kind() != AnnotationTarget.Kind.METHOD) { // we only care about the annotations on the method itself
                        continue;
                    }
                    if (annotation.name().toString().contains("json.bind")) {
                        jsonbAnnotationCount++;
                    }
                }
                if (jsonbAnnotationCount > 1) {
                    // we bail out and let jsonb handle this case at runtime (which will end up throwing an exception)
                    return true;
                } else {
                    // the field was annotated with the @JsonbTransient so we ignore it
                    iterator.remove();
                }
            }
            if (next.getValue() != null && next.getValue().hasAnnotation(DotNames.JSONB_TRANSIENT)) {
                int jsonbAnnotationCount = 0;
                for (AnnotationInstance annotation : next.getValue().annotations()) {
                    if (annotation.name().toString().contains("json.bind")) {
                        jsonbAnnotationCount++;
                    }
                }
                if (jsonbAnnotationCount > 1) {
                    // we bail out and let jsonb handle this case at runtime (which will end up throwing an exception)
                    return true;
                } else {
                    // the field was annotated with the @JsonbTransient so we ignore it
                    iterator.remove();
                }
            }
        }
        return false;
    }

    // check if type is the same as the class type or is a generic type that references it
    private boolean hasReferenceToClassType(Type type, DotName classDotName) {
        if (type.name().equals(classDotName)) {
            // we don't support serializing types that contain serializable items of the same class
            return true;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = type.asParameterizedType();
            for (Type argumentType : parameterizedType.arguments()) {
                if (hasReferenceToClassType(argumentType, classDotName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public IndexView getIndex() {
        return index;
    }

    public static class Result {
        private final ClassInfo classInfo;
        private final boolean isPossible;
        private final Map<DotName, AnnotationInstance> effectiveClassAnnotations;
        private final Map<MethodInfo, FieldInfo> getters;
        private final Collection<FieldInfo> visibleFieldsWithoutGetters;

        private Result(ClassInfo classInfo, boolean isPossible,
                Map<DotName, AnnotationInstance> effectiveClassAnnotations,
                Map<MethodInfo, FieldInfo> getters, Collection<FieldInfo> visibleFieldsWithoutGetters) {
            this.classInfo = classInfo;
            this.isPossible = isPossible;
            this.effectiveClassAnnotations = effectiveClassAnnotations;
            this.getters = getters;
            this.visibleFieldsWithoutGetters = visibleFieldsWithoutGetters;
        }

        public static Result notPossible(ClassInfo classInfo) {
            return new Result(classInfo, false, Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyList());
        }

        public static Result possible(ClassInfo classInfo, Map<DotName, AnnotationInstance> effectiveClassAnnotations,
                Map<MethodInfo, FieldInfo> getters, Collection<FieldInfo> visibleFieldsWithoutGetters) {
            return new Result(classInfo, true, effectiveClassAnnotations, getters, visibleFieldsWithoutGetters);
        }

        public ClassInfo getClassInfo() {
            return classInfo;
        }

        public boolean isPossible() {
            return isPossible;
        }

        public Map<DotName, AnnotationInstance> getEffectiveClassAnnotations() {
            return effectiveClassAnnotations;
        }

        public Map<MethodInfo, FieldInfo> getGetters() {
            return getters;
        }

        public Collection<FieldInfo> getVisibleFieldsWithoutGetters() {
            return visibleFieldsWithoutGetters;
        }
    }
}

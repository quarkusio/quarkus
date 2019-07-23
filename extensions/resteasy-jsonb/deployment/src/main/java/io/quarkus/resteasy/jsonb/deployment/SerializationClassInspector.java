package io.quarkus.resteasy.jsonb.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
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
        return inspect(classDotName, true);
    }

    private Result inspect(DotName classDotName, boolean checkSubtypes) {
        ClassInfo classInfo = index.getClassByName(classDotName);
        if (classInfo == null) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        if (!Modifier.isPublic(classInfo.flags())) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        if (Modifier.isInterface(classInfo.flags())) {
            Collection<ClassInfo> allKnownImplementors = index.getAllKnownImplementors(classInfo.name());
            if (allKnownImplementors.size() != 1) {
                // when the type is an interface than we can correctly generate a serializer at build time
                // if there is a single implementation
                // TODO investigate if this can possible be relaxed by checking and comparing all fields, getters and
                //  class annotations of the implementations or perhaps by generating serializers for all implementations?
                return SerializationClassInspector.Result.notPossible(classInfo);
            } else {
                return inspect(allKnownImplementors.iterator().next().name());
            }
        }

        if (checkSubtypes && !index.getAllKnownSubclasses(classDotName).isEmpty()) {
            // if the class is subclassed  we ignore it because json-b
            // adds all the properties of the implementation or subclasses (which we can't know)
            // TODO investigate if we could relax these constraints by checking if there
            //  there are no subclasses that contain properties other than those
            //  of the interface or class. Another idea is to generate serializers for all subclasses
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        if (classInspectionResultMap.containsKey(classInfo.name())) {
            return classInspectionResultMap.get(classInfo.name());
        }

        Result superClassResult = null;
        if (!DotNames.OBJECT.equals(classInfo.superName())) {
            superClassResult = inspect(classInfo.superName(), false);
            if (!superClassResult.isPossible()) {
                return SerializationClassInspector.Result.notPossible(classInfo);
            }
            classInspectionResultMap.put(classInfo.superName(), superClassResult);
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

        List<FieldInfo> fields = PropertyUtil.getPublicFieldsWithoutGetters(classInfo, getters.keySet());
        if (removeTransientFields(fields)) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        // we don't support generating a serializer when the class to be serialized is referenced as a field
        // or getter
        if (hasCyclicReferences(getters.keySet(), fields, classDotName)) {
            return SerializationClassInspector.Result.notPossible(classInfo);
        }

        SerializationClassInspector.Result result = SerializationClassInspector.Result.possible(classInfo,
                effectiveClassAnnotations, getters, fields);

        if (superClassResult != null) {
            result = result.merge(superClassResult);
        }

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

    // checks whether the methods or fields contain any references back to the class
    // this check is recursive meaning that the methods and fields are also checked in turn for cyclic references
    private boolean hasCyclicReferences(Collection<MethodInfo> methods, Collection<FieldInfo> fields, DotName classDotName) {
        return hasCyclicReferences(methods, fields, Collections.singleton(classDotName));
    }

    private boolean hasCyclicReferences(Collection<MethodInfo> methods, Collection<FieldInfo> fields, Set<DotName> candidates) {
        Set<DotName> additionalTypesToCheck = new HashSet<>();
        // first check if any of the fields or methods contain direct references to the candidates
        for (MethodInfo method : methods) {
            if (containedInCandidates(method.returnType(), candidates, additionalTypesToCheck)) {
                return true;
            }
        }
        for (FieldInfo field : fields) {
            if (containedInCandidates(field.type(), candidates, additionalTypesToCheck)) {
                return true;
            }
        }
        // now recursively check the class types of fields and methods to see if any of their
        // fields or methods contain references
        for (DotName dotName : additionalTypesToCheck) {
            ClassInfo classInfo = index.getClassByName(dotName);
            if (classInfo == null) {
                continue;
            }
            Set<MethodInfo> newMethods = PropertyUtil.getGetterMethods(classInfo).keySet();
            List<FieldInfo> newFields = PropertyUtil.getPublicFieldsWithoutGetters(classInfo, newMethods);
            Set<DotName> newCandidates = new HashSet<>(candidates);
            newCandidates.add(classInfo.name());
            if (hasCyclicReferences(newMethods, newFields, newCandidates)) {
                return true;
            }
        }

        return false;
    }

    private boolean containedInCandidates(Type type, Set<DotName> candidates, Set<DotName> additionalTypesToCheck) {
        if (type instanceof ClassType) {
            if (candidates.contains(type.name())) {
                return true;
            } else {
                additionalTypesToCheck.add(type.name());
            }
        } else if (type instanceof ParameterizedType) {
            List<Type> argumentTypes = type.asParameterizedType().arguments();
            for (Type argumentType : argumentTypes) {
                if (containedInCandidates(argumentType, candidates, additionalTypesToCheck)) {
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

        /**
         * Merge info from other result with lower priority, which means that info from this object
         * takes precedence if conflicting data exists
         */
        public Result merge(Result lowerPriorityResult) {
            if (!(this.isPossible && lowerPriorityResult.isPossible)) {
                throw new IllegalArgumentException("merge can only be used on Result objects who have isPossible = true");
            }

            Map<DotName, AnnotationInstance> finalEffectiveClassAnnotations = new HashMap<>(effectiveClassAnnotations);
            for (DotName dotName : lowerPriorityResult.getEffectiveClassAnnotations().keySet()) {
                if (!finalEffectiveClassAnnotations.containsKey(dotName)) {
                    finalEffectiveClassAnnotations.put(dotName,
                            lowerPriorityResult.getEffectiveClassAnnotations().get(dotName));
                }
            }

            Map<MethodInfo, FieldInfo> finalGetters = new HashMap<>(getters);
            Set<String> getterNames = new HashSet<>(getters.size());
            for (MethodInfo methodInfo : getters.keySet()) {
                getterNames.add(methodInfo.name());
            }
            for (MethodInfo methodInfo : lowerPriorityResult.getGetters().keySet()) {
                if (!getterNames.contains(methodInfo.name())) {
                    finalGetters.put(methodInfo, lowerPriorityResult.getGetters().get(methodInfo));
                }
            }

            Collection<FieldInfo> finalVisibleFieldsWithoutGetters = new ArrayList<>(visibleFieldsWithoutGetters);
            Set<String> fieldNames = new HashSet<>(visibleFieldsWithoutGetters.size());
            for (FieldInfo fieldInfo : visibleFieldsWithoutGetters) {
                fieldNames.add(fieldInfo.name());
            }
            for (FieldInfo fieldInfo : lowerPriorityResult.getVisibleFieldsWithoutGetters()) {
                if (!fieldNames.contains(fieldInfo.name())) {
                    finalVisibleFieldsWithoutGetters.add(fieldInfo);
                }
            }

            return new Result(this.classInfo, true, finalEffectiveClassAnnotations, finalGetters,
                    finalVisibleFieldsWithoutGetters);
        }
    }
}

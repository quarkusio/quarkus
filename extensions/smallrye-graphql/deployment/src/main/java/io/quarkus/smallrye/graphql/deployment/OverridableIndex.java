package io.quarkus.smallrye.graphql.deployment;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ModuleInfo;
import org.jboss.jandex.RecordComponentInfo;
import org.jboss.jandex.Type;

public class OverridableIndex implements IndexView {

    private final IndexView original;
    private final IndexView override;

    private OverridableIndex(IndexView original, IndexView override) {
        this.original = original;
        this.override = override;
    }

    public static OverridableIndex create(IndexView original, IndexView override) {
        return new OverridableIndex(original, override);
    }

    @Override
    public Collection<ClassInfo> getKnownClasses() {
        return overrideCollection(original.getKnownClasses(), override.getKnownClasses(), classInfoComparator);
    }

    @Override
    public ClassInfo getClassByName(DotName dn) {
        return overrideObject(original.getClassByName(dn), override.getClassByName(dn));
    }

    @Override
    public Collection<ClassInfo> getKnownDirectSubclasses(DotName dn) {
        return overrideCollection(original.getKnownDirectSubclasses(dn), override.getKnownDirectSubclasses(dn),
                classInfoComparator);
    }

    @Override
    public Collection<ClassInfo> getAllKnownSubclasses(DotName dn) {
        return overrideCollection(original.getAllKnownSubclasses(dn), override.getAllKnownSubclasses(dn), classInfoComparator);
    }

    @Override
    public Collection<ClassInfo> getKnownDirectImplementors(DotName dn) {
        return overrideCollection(original.getKnownDirectImplementors(dn), override.getKnownDirectImplementors(dn),
                classInfoComparator);
    }

    @Override
    public Collection<ClassInfo> getAllKnownImplementors(DotName dn) {
        return overrideCollection(original.getAllKnownImplementors(dn), override.getAllKnownImplementors(dn),
                classInfoComparator);
    }

    @Override
    public Collection<AnnotationInstance> getAnnotations(DotName dn) {
        return overrideCollection(original.getAnnotations(dn), override.getAnnotations(dn), annotationInstanceComparator);
    }

    @Override
    public Collection<AnnotationInstance> getAnnotationsWithRepeatable(DotName dn, IndexView iv) {
        return overrideCollection(original.getAnnotationsWithRepeatable(dn, iv), override.getAnnotationsWithRepeatable(dn, iv),
                annotationInstanceComparator);
    }

    @Override
    public Collection<ModuleInfo> getKnownModules() {
        return overrideCollection(original.getKnownModules(), override.getKnownModules(), moduleInfoComparator);
    }

    @Override
    public ModuleInfo getModuleByName(DotName dn) {
        return overrideObject(original.getModuleByName(dn), override.getModuleByName(dn));
    }

    @Override
    public Collection<ClassInfo> getKnownUsers(DotName dn) {
        return overrideCollection(original.getKnownUsers(dn), override.getKnownUsers(dn), classInfoComparator);
    }

    private Comparator<ClassInfo> classInfoComparator = new Comparator<ClassInfo>() {
        @Override
        public int compare(ClassInfo t, ClassInfo t1) {
            return t.name().toString().compareTo(t1.name().toString());
        }
    };

    private Comparator<Type> typeComparator = new Comparator<Type>() {
        @Override
        public int compare(Type t, Type t1) {
            return t.name().toString().compareTo(t1.name().toString());
        }
    };

    private Comparator<ModuleInfo> moduleInfoComparator = new Comparator<ModuleInfo>() {
        @Override
        public int compare(ModuleInfo t, ModuleInfo t1) {
            return t.name().toString().compareTo(t1.name().toString());
        }
    };

    private Comparator<FieldInfo> fieldInfoComparator = new Comparator<FieldInfo>() {
        @Override
        public int compare(FieldInfo t, FieldInfo t1) {
            if (classInfoComparator.compare(t.declaringClass(), t1.declaringClass()) == 0) { // Same class
                return t.name().toString().compareTo(t1.name().toString());
            }
            return -1;
        }
    };

    private Comparator<RecordComponentInfo> recordComponentInfoComparator = new Comparator<RecordComponentInfo>() {
        @Override
        public int compare(RecordComponentInfo t, RecordComponentInfo t1) {
            if (classInfoComparator.compare(t.declaringClass(), t1.declaringClass()) == 0) { // Same class
                return t.name().toString().compareTo(t1.name().toString());
            }
            return -1;
        }
    };

    private Comparator<MethodInfo> methodInfoComparator = new Comparator<MethodInfo>() {
        @Override
        public int compare(MethodInfo t, MethodInfo t1) {
            if (classInfoComparator.compare(t.declaringClass(), t1.declaringClass()) == 0) { // Same class
                if (t.name().toString().compareTo(t1.name().toString()) == 0) { // Same method name
                    if (t.parameters().size() == t1.parameters().size()) { // Same number of parameters
                        for (int i = 0; i < t.parameters().size(); i++) {
                            int typeTheSame = typeComparator.compare(t.parameters().get(i), t1.parameters().get(i));
                            if (typeTheSame != 0) {
                                return typeTheSame;
                            }
                        }
                        // All parameter type are the same
                        return 0;
                    }
                }
            }
            return -1;
        }
    };

    private Comparator<MethodParameterInfo> methodParameterInfoComparator = new Comparator<MethodParameterInfo>() {
        @Override
        public int compare(MethodParameterInfo t, MethodParameterInfo t1) {
            if (methodInfoComparator.compare(t.method(), t1.method()) == 0 && // Same method
                    t.kind().equals(t1.kind()) && // Same kind
                    t.name().equals(t1.name()) && // Same name
                    t.position() == t1.position()) { // Same position
                return 0;
            }
            return -1;
        }
    };

    private Comparator<AnnotationInstance> annotationInstanceComparator = new Comparator<AnnotationInstance>() {
        @Override
        public int compare(AnnotationInstance t, AnnotationInstance t1) {
            if (t.name().equals(t1.name())) {
                // Class Info
                if (t.target().kind().equals(AnnotationTarget.Kind.CLASS)
                        && t1.target().kind().equals(AnnotationTarget.Kind.CLASS)) {
                    return classInfoComparator.compare(t.target().asClass(), t1.target().asClass());
                }

                // Field Info
                if (t.target().kind().equals(AnnotationTarget.Kind.FIELD)
                        && t1.target().kind().equals(AnnotationTarget.Kind.FIELD)) {
                    return fieldInfoComparator.compare(t.target().asField(), t1.target().asField());
                }

                // Type
                if (t.target().kind().equals(AnnotationTarget.Kind.TYPE)
                        && t1.target().kind().equals(AnnotationTarget.Kind.TYPE)) {
                    return typeComparator.compare(t.target().asType().target(), t1.target().asType().target());
                }

                // Method Info
                if (t.target().kind().equals(AnnotationTarget.Kind.METHOD)
                        && t1.target().kind().equals(AnnotationTarget.Kind.METHOD)) {
                    return methodInfoComparator.compare(t.target().asMethod(), t1.target().asMethod());
                }

                // Method Parameter
                if (t.target().kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)
                        && t1.target().kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)) {
                    return methodParameterInfoComparator.compare(t.target().asMethodParameter(),
                            t1.target().asMethodParameter());
                }

                // Record
                if (t.target().kind().equals(AnnotationTarget.Kind.RECORD_COMPONENT)
                        && t1.target().kind().equals(AnnotationTarget.Kind.RECORD_COMPONENT)) {
                    return recordComponentInfoComparator.compare(t.target().asRecordComponent(),
                            t1.target().asRecordComponent());
                }
            }
            return -1;
        }
    };

    private <T> Collection<T> overrideCollection(Collection<T> originalCollection, Collection<T> overrideCollection,
            Comparator<T> comparator) {
        if (originalCollection == null && overrideCollection == null) {
            return null;
        }

        if (originalCollection == null) {
            return overrideCollection;
        }
        if (overrideCollection == null) {
            return originalCollection;
        }

        if (originalCollection.isEmpty() && overrideCollection.isEmpty()) {
            return originalCollection;
        }

        if (originalCollection.isEmpty()) {
            return overrideCollection;
        }
        if (overrideCollection.isEmpty()) {
            return originalCollection;
        }

        Set<T> newCollection = new TreeSet<>(comparator);
        newCollection.addAll(overrideCollection);
        newCollection.addAll(originalCollection); // Won't add if it's already there.
        return newCollection;
    }

    private <T> T overrideObject(T originalObject, T overrideObject) {
        if (overrideObject != null) {
            return overrideObject;
        }
        return originalObject;
    }
}

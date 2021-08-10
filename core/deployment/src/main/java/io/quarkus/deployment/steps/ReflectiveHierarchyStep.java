package io.quarkus.deployment.steps;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.UnresolvedTypeVariable;
import org.jboss.jandex.VoidType;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassFinalFieldsWritablePredicateBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.util.JandexUtil;

public class ReflectiveHierarchyStep {

    private static final Logger log = Logger.getLogger(ReflectiveHierarchyStep.class);

    @FunctionalInterface
    private interface ReflectiveHierarchyVisitor {
        void visit() throws Exception;
    }

    @BuildStep
    public ReflectiveHierarchyIgnoreWarningBuildItem ignoreJavaClassWarnings() {
        return new ReflectiveHierarchyIgnoreWarningBuildItem(ReflectiveHierarchyBuildItem.IgnoreWhiteListedPredicate.INSTANCE);
    }

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndexBuildItem,
            List<ReflectiveHierarchyBuildItem> hierarchy,
            List<ReflectiveHierarchyIgnoreWarningBuildItem> ignored,
            List<ReflectiveClassFinalFieldsWritablePredicateBuildItem> finalFieldsWritablePredicates,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        Set<DotName> processedReflectiveHierarchies = new HashSet<>();
        Map<DotName, Set<String>> unindexedClasses = new TreeMap<>();

        final Predicate<ClassInfo> finalFieldsWritable = finalFieldsWritablePredicates.isEmpty() ?
        // no need to make final fields writable by default
                (c) -> false
                :
                // create a predicate that returns true if any of the predicates says that final fields need to be writable
                finalFieldsWritablePredicates
                        .stream()
                        .map(ReflectiveClassFinalFieldsWritablePredicateBuildItem::getPredicate)
                        .reduce(c -> false, Predicate::or);

        // to avoid recursive processing of the hierarchy (which could lead to a StackOverflowError) we are going to be scheduling type visits instead
        final Deque<ReflectiveHierarchyVisitor> visits = new ArrayDeque<>();

        for (ReflectiveHierarchyBuildItem i : hierarchy) {
            addReflectiveHierarchy(combinedIndexBuildItem,
                    i,
                    i.hasSource() ? i.getSource() : i.getType().name().toString(),
                    i.getType(),
                    processedReflectiveHierarchies,
                    unindexedClasses,
                    finalFieldsWritable, reflectiveClass, visits);
        }

        while (!visits.isEmpty()) {
            visits.removeFirst().visit();
        }

        removeIgnored(unindexedClasses, ignored);

        if (!unindexedClasses.isEmpty()) {
            StringBuilder unindexedClassesWarn = new StringBuilder();
            for (Entry<DotName, Set<String>> unindexedClassEntry : unindexedClasses.entrySet()) {
                if (unindexedClassesWarn.length() != 0) {
                    unindexedClassesWarn.append("\n");
                }
                unindexedClassesWarn.append("\t- ").append(unindexedClassEntry.getKey());
                unindexedClassesWarn.append(" (source");
                if (unindexedClassEntry.getValue().size() > 1) {
                    unindexedClassesWarn.append("s");
                }
                unindexedClassesWarn.append(": ");
                unindexedClassesWarn.append(String.join(", ", unindexedClassEntry.getValue().toArray(new String[0])));
                unindexedClassesWarn.append(")");
            }
            log.warnf(
                    "Unable to properly register the hierarchy of the following classes for reflection as they are not in the Jandex index:%n%s"
                            + "%nConsider adding them to the index either by creating a Jandex index "
                            + "for your dependency via the Maven plugin, an empty META-INF/beans.xml or quarkus.index-dependency properties.",
                    unindexedClassesWarn.toString());
        }
    }

    private void removeIgnored(Map<DotName, Set<String>> unindexedClasses,
            List<ReflectiveHierarchyIgnoreWarningBuildItem> ignored) {
        if (ignored.isEmpty()) {
            return;
        }
        // the final predicate ignores a DotName if and only if at least one of the predicates indicates that warning should be ignored
        Predicate<DotName> finalPredicate = ignored.stream().map(ReflectiveHierarchyIgnoreWarningBuildItem::getPredicate)
                .reduce(x -> false, Predicate::or);
        Set<DotName> unindexedDotNames = new HashSet<>(unindexedClasses.keySet());
        for (DotName unindexedDotName : unindexedDotNames) {
            if (finalPredicate.test(unindexedDotName)) {
                unindexedClasses.remove(unindexedDotName);
            }
        }
    }

    private void addReflectiveHierarchy(CombinedIndexBuildItem combinedIndexBuildItem,
            ReflectiveHierarchyBuildItem reflectiveHierarchyBuildItem, String source, Type type,
            Set<DotName> processedReflectiveHierarchies, Map<DotName, Set<String>> unindexedClasses,
            Predicate<ClassInfo> finalFieldsWritable, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            Deque<ReflectiveHierarchyVisitor> visits) {
        if (type instanceof VoidType ||
                type instanceof PrimitiveType ||
                type instanceof UnresolvedTypeVariable) {
            return;
        } else if (type instanceof ClassType) {
            if (reflectiveHierarchyBuildItem.getIgnoreTypePredicate().test(type.name())) {
                return;
            }

            addClassTypeHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source, type.name(), type.name(),
                    processedReflectiveHierarchies, unindexedClasses,
                    finalFieldsWritable, reflectiveClass, visits);

            for (ClassInfo subclass : combinedIndexBuildItem.getIndex().getAllKnownSubclasses(type.name())) {
                addClassTypeHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source, subclass.name(),
                        subclass.name(),
                        processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable, reflectiveClass, visits);
            }
            for (ClassInfo subclass : combinedIndexBuildItem.getIndex().getAllKnownImplementors(type.name())) {
                addClassTypeHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source, subclass.name(),
                        subclass.name(),
                        processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable, reflectiveClass, visits);
            }
        } else if (type instanceof ArrayType) {
            visits.addLast(() -> addReflectiveHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source,
                    type.asArrayType().component(),
                    processedReflectiveHierarchies,
                    unindexedClasses, finalFieldsWritable, reflectiveClass, visits));
        } else if (type instanceof ParameterizedType) {
            if (!reflectiveHierarchyBuildItem.getIgnoreTypePredicate().test(type.name())) {
                addClassTypeHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source, type.name(),
                        type.name(),
                        processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable, reflectiveClass, visits);
            }
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            for (Type typeArgument : parameterizedType.arguments()) {
                visits.addLast(
                        () -> addReflectiveHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source, typeArgument,
                                processedReflectiveHierarchies,
                                unindexedClasses, finalFieldsWritable, reflectiveClass, visits));
            }
        }
    }

    private void addClassTypeHierarchy(CombinedIndexBuildItem combinedIndexBuildItem,
            ReflectiveHierarchyBuildItem reflectiveHierarchyBuildItem,
            String source,
            DotName name,
            DotName initialName,
            Set<DotName> processedReflectiveHierarchies,
            Map<DotName, Set<String>> unindexedClasses,
            Predicate<ClassInfo> finalFieldsWritable,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            Deque<ReflectiveHierarchyVisitor> visits) {
        if (name == null) {
            return;
        }
        if (reflectiveHierarchyBuildItem.getIgnoreTypePredicate().test(name)) {
            return;
        }

        ClassInfo info = (reflectiveHierarchyBuildItem.getIndex() != null ? reflectiveHierarchyBuildItem.getIndex()
                : combinedIndexBuildItem.getIndex()).getClassByName(name);

        if (info == null) {
            unindexedClasses.putIfAbsent(name, new TreeSet<>());
            unindexedClasses.get(name).add(source);
        }

        if (processedReflectiveHierarchies.contains(name)) {
            return;
        }

        reflectiveClass.produce(
                ReflectiveClassBuildItem
                        .builder(name.toString())
                        .methods(true)
                        .fields(true)
                        .finalFieldsWritable(doFinalFieldsNeedToBeWritable(info, finalFieldsWritable))
                        .serialization(reflectiveHierarchyBuildItem.isSerialization())
                        .build());

        processedReflectiveHierarchies.add(name);

        if (info == null) {
            return;
        }

        visits.addLast(() -> addClassTypeHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source,
                info.superName(), initialName,
                processedReflectiveHierarchies,
                unindexedClasses, finalFieldsWritable, reflectiveClass, visits));
        for (FieldInfo field : info.fields()) {
            if (reflectiveHierarchyBuildItem.getIgnoreFieldPredicate().test(field) ||
            // skip the static fields (especially loggers)
                    Modifier.isStatic(field.flags()) ||
                    // also skip the outer class elements (unfortunately, we don't have a way to test for synthetic fields in Jandex)
                    field.name().startsWith("this$") || field.name().startsWith("val$")) {
                continue;
            }
            final Type fieldType = getFieldType(combinedIndexBuildItem, initialName, info, field);
            visits.addLast(
                    () -> addReflectiveHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source, fieldType,
                            processedReflectiveHierarchies,
                            unindexedClasses, finalFieldsWritable, reflectiveClass, visits));
        }
        for (MethodInfo method : info.methods()) {
            if (reflectiveHierarchyBuildItem.getIgnoreMethodPredicate().test(method) ||
            // we will only consider potential getters
                    method.parameters().size() > 0 ||
                    Modifier.isStatic(method.flags()) ||
                    method.returnType().kind() == Kind.VOID) {
                continue;
            }
            visits.addLast(() -> addReflectiveHierarchy(combinedIndexBuildItem, reflectiveHierarchyBuildItem, source,
                    method.returnType(),
                    processedReflectiveHierarchies,
                    unindexedClasses, finalFieldsWritable, reflectiveClass, visits));
        }
    }

    private static Type getFieldType(CombinedIndexBuildItem combinedIndexBuildItem, DotName initialName, ClassInfo info,
            FieldInfo field) {
        Type fieldType = field.type();
        if ((field.type().kind() == Kind.TYPE_VARIABLE) && (info.typeParameters().size() == 1)) {
            // handle the common case where the super type has a generic type in the class signature which
            // is completely resolved by the sub type
            // this could be made to handle more complex cases, but it is unlikely we will have to do so
            if (field.type().asTypeVariable().identifier().equals(info.typeParameters().get(0).identifier())) {
                try {
                    List<Type> types = JandexUtil.resolveTypeParameters(initialName, info.name(),
                            combinedIndexBuildItem.getIndex());
                    if (types.size() == 1) {
                        fieldType = types.get(0);
                    }
                } catch (IllegalArgumentException ignored) {

                }
            }
        }
        return fieldType;
    }

    private static boolean doFinalFieldsNeedToBeWritable(ClassInfo classInfo, Predicate<ClassInfo> finalFieldsWritable) {
        if (classInfo == null) {
            return false;
        }
        return finalFieldsWritable.test(classInfo);
    }
}

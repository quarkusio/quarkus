package io.quarkus.deployment.steps;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

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

public class ReflectiveHierarchyStep {

    private static final Logger log = Logger.getLogger(ReflectiveHierarchyStep.class);

    @Inject
    List<ReflectiveHierarchyBuildItem> hierarchy;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    List<ReflectiveHierarchyIgnoreWarningBuildItem> ignored;

    @Inject
    List<ReflectiveClassFinalFieldsWritablePredicateBuildItem> finalFieldsWritablePredicates;

    @BuildStep
    public void build() throws Exception {
        Set<DotName> processedReflectiveHierarchies = new HashSet<>();
        Set<DotName> unindexedClasses = new TreeSet<>();

        Predicate<ClassInfo> finalFieldsWritable = (c) -> false; // no need to make final fields writable by default
        if (!finalFieldsWritablePredicates.isEmpty()) {
            // create a predicate that returns true if any of the predicates says that final fields need to be writable
            finalFieldsWritable = finalFieldsWritablePredicates
                    .stream()
                    .map(ReflectiveClassFinalFieldsWritablePredicateBuildItem::getPredicate)
                    .reduce(c -> false, Predicate::or);
        }

        for (ReflectiveHierarchyBuildItem i : hierarchy) {
            addReflectiveHierarchy(i, i.getType(), processedReflectiveHierarchies, unindexedClasses, finalFieldsWritable);
        }
        for (ReflectiveHierarchyIgnoreWarningBuildItem i : ignored) {
            unindexedClasses.remove(i.getDotName());
        }

        if (!unindexedClasses.isEmpty()) {
            String unindexedClassesWarn = unindexedClasses.stream().map(d -> "\t- " + d).collect(Collectors.joining("\n"));
            log.warnf(
                    "Unable to properly register the hierarchy of the following classes for reflection as they are not in the Jandex index:%n%s"
                            + "%nConsider adding them to the index either by creating a Jandex index "
                            + "for your dependency via the Maven plugin, an empty META-INF/beans.xml or quarkus.index-dependency properties.\");.",
                    unindexedClassesWarn);
        }
    }

    private void addReflectiveHierarchy(ReflectiveHierarchyBuildItem reflectiveHierarchyBuildItem, Type type,
            Set<DotName> processedReflectiveHierarchies, Set<DotName> unindexedClasses,
            Predicate<ClassInfo> finalFieldsWritable) {
        if (type instanceof VoidType ||
                type instanceof PrimitiveType ||
                type instanceof UnresolvedTypeVariable) {
            return;
        } else if (type instanceof ClassType) {
            if (skipClass(type.name(), reflectiveHierarchyBuildItem.getIgnorePredicate(), processedReflectiveHierarchies)) {
                return;
            }

            addClassTypeHierarchy(reflectiveHierarchyBuildItem, type.name(), processedReflectiveHierarchies, unindexedClasses,
                    finalFieldsWritable);

            for (ClassInfo subclass : combinedIndexBuildItem.getIndex().getAllKnownSubclasses(type.name())) {
                addClassTypeHierarchy(reflectiveHierarchyBuildItem, subclass.name(), processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable);
            }
            for (ClassInfo subclass : combinedIndexBuildItem.getIndex().getAllKnownImplementors(type.name())) {
                addClassTypeHierarchy(reflectiveHierarchyBuildItem, subclass.name(), processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable);
            }
        } else if (type instanceof ArrayType) {
            addReflectiveHierarchy(reflectiveHierarchyBuildItem, type.asArrayType().component(), processedReflectiveHierarchies,
                    unindexedClasses, finalFieldsWritable);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (!reflectiveHierarchyBuildItem.getIgnorePredicate().test(parameterizedType.name())) {
                addClassTypeHierarchy(reflectiveHierarchyBuildItem, parameterizedType.name(), processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable);
            }
            for (Type typeArgument : parameterizedType.arguments()) {
                addReflectiveHierarchy(reflectiveHierarchyBuildItem, typeArgument, processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable);
            }
        }
    }

    private void addClassTypeHierarchy(ReflectiveHierarchyBuildItem reflectiveHierarchyBuildItem, DotName name,
            Set<DotName> processedReflectiveHierarchies,
            Set<DotName> unindexedClasses, Predicate<ClassInfo> finalFieldsWritable) {
        if (skipClass(name, reflectiveHierarchyBuildItem.getIgnorePredicate(), processedReflectiveHierarchies)) {
            return;
        }
        processedReflectiveHierarchies.add(name);

        ClassInfo info = (reflectiveHierarchyBuildItem.getIndex() != null ? reflectiveHierarchyBuildItem.getIndex()
                : combinedIndexBuildItem.getIndex()).getClassByName(name);
        reflectiveClass.produce(
                ReflectiveClassBuildItem
                        .builder(name.toString())
                        .methods(true)
                        .fields(true)
                        .finalFieldsWritable(doFinalFieldsNeedToBeWritable(info, finalFieldsWritable))
                        .build());
        if (info == null) {
            unindexedClasses.add(name);
        } else {
            addClassTypeHierarchy(reflectiveHierarchyBuildItem, info.superName(), processedReflectiveHierarchies,
                    unindexedClasses, finalFieldsWritable);
            for (FieldInfo field : info.fields()) {
                if (Modifier.isStatic(field.flags()) || field.name().startsWith("this$") || field.name().startsWith("val$")) {
                    // skip the static fields (especially loggers)
                    // also skip the outer class elements (unfortunately, we don't have a way to test for synthetic fields in Jandex)
                    continue;
                }
                addReflectiveHierarchy(reflectiveHierarchyBuildItem, field.type(), processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable);
            }
            for (MethodInfo method : info.methods()) {
                if (method.parameters().size() > 0 || Modifier.isStatic(method.flags())
                        || method.returnType().kind() == Kind.VOID) {
                    // we will only consider potential getters
                    continue;
                }
                addReflectiveHierarchy(reflectiveHierarchyBuildItem, method.returnType(), processedReflectiveHierarchies,
                        unindexedClasses, finalFieldsWritable);
            }
        }
    }

    private boolean skipClass(DotName name, Predicate<DotName> ignorePredicate, Set<DotName> processedReflectiveHierarchies) {
        return ignorePredicate.test(name) || processedReflectiveHierarchies.contains(name);
    }

    private boolean doFinalFieldsNeedToBeWritable(ClassInfo classInfo, Predicate<ClassInfo> finalFieldsWritable) {
        if (classInfo == null) {
            return false;
        }
        return finalFieldsWritable.test(classInfo);
    }
}

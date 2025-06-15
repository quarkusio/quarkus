package io.quarkus.arc.deployment;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class AutoAddScopeProcessor {

    private static final Logger LOGGER = Logger.getLogger(AutoAddScopeProcessor.class);

    @BuildStep
    void annotationTransformer(List<AutoAddScopeBuildItem> autoScopes, CustomScopeAnnotationsBuildItem scopes,
            List<AutoInjectAnnotationBuildItem> autoInjectAnnotations,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformers,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans, BeanArchiveIndexBuildItem beanArchiveIndex)
            throws Exception {
        if (autoScopes.isEmpty()) {
            return;
        }

        List<AutoAddScopeBuildItem> sortedAutoScopes = autoScopes.stream()
                .sorted(Comparator.comparingInt(AutoAddScopeBuildItem::getPriority).reversed())
                .collect(Collectors.toList());

        Set<DotName> containerAnnotationNames = autoInjectAnnotations.stream()
                .flatMap(a -> a.getAnnotationNames().stream()).collect(Collectors.toSet());
        containerAnnotationNames.add(DotNames.POST_CONSTRUCT);
        containerAnnotationNames.add(DotNames.PRE_DESTROY);
        containerAnnotationNames.add(DotNames.INJECT);

        ConcurrentMap<DotName, AutoAddScopeBuildItem> unremovables = sortedAutoScopes.stream()
                .anyMatch(AutoAddScopeBuildItem::isUnremovable) ? new ConcurrentHashMap<>() : null;

        annotationsTransformers.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(Kind kind) {
                return kind == Kind.CLASS;
            }

            @Override
            public int getPriority() {
                // Make sure this annotation transformer runs before all transformers with the default priority
                return DEFAULT_PRIORITY + 1000;
            }

            @Override
            public void transform(TransformationContext context) {
                if (scopes.isScopeIn(context.getAnnotations())) {
                    // Skip classes annotated with a scope
                    return;
                }
                ClassInfo clazz = context.getTarget().asClass();
                DotName scope = null;
                Boolean requiresContainerServices = null;
                String reason = null;

                for (AutoAddScopeBuildItem autoScope : sortedAutoScopes) {
                    if (autoScope.isContainerServicesRequired()) {
                        if (requiresContainerServices == null) {
                            // Analyze the class hierarchy lazily
                            requiresContainerServices = requiresContainerServices(clazz, containerAnnotationNames,
                                    beanArchiveIndex.getIndex());
                        }
                        if (!requiresContainerServices) {
                            // Skip - no injection point detected
                            continue;
                        }
                    }
                    if (autoScope.test(clazz, context.getAnnotations(), beanArchiveIndex.getIndex())) {
                        if (scope != null) {
                            BiConsumer<DotName, String> consumer = autoScope.getScopeAlreadyAdded();
                            if (consumer != null) {
                                consumer.accept(scope, reason);
                            } else {
                                LOGGER.debugf("Scope %s was already added for reason: %s", scope, reason);
                            }
                            continue;
                        }
                        scope = autoScope.getDefaultScope();
                        reason = autoScope.getReason();
                        context.transform().add(scope).done();
                        if (unremovables != null && autoScope.isUnremovable()) {
                            unremovables.put(clazz.name(), autoScope);
                        }
                        LOGGER.debugf("Automatically added scope %s to class %s: %s", scope, clazz,
                                autoScope.getReason());
                    }
                }
            }
        }));

        if (unremovables != null) {
            unremovableBeans.produce(new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {

                @Override
                public boolean test(BeanInfo bean) {
                    return bean.isClassBean() && unremovables.containsKey(bean.getBeanClass());
                }
            }));
        }
    }

    private boolean requiresContainerServices(ClassInfo clazz, Set<DotName> containerAnnotationNames, IndexView index) {
        // Note that transformed methods/fields are not taken into account
        if (hasContainerAnnotation(clazz, containerAnnotationNames)) {
            return true;
        }
        if (index != null) {
            DotName superName = clazz.superName();
            while (superName != null && !superName.equals(DotNames.OBJECT)) {
                final ClassInfo superClass = index.getClassByName(superName);
                if (superClass != null) {
                    if (hasContainerAnnotation(superClass, containerAnnotationNames)) {
                        return true;
                    }
                    superName = superClass.superName();
                } else {
                    superName = null;
                }
            }
        }
        return false;
    }

    private boolean hasContainerAnnotation(ClassInfo clazz, Set<DotName> containerAnnotationNames) {
        if (clazz.annotationsMap().isEmpty() || containerAnnotationNames.isEmpty()) {
            return false;
        }
        return containsAny(clazz, containerAnnotationNames);
    }

    private boolean containsAny(ClassInfo clazz, Set<DotName> annotationNames) {
        for (DotName annotation : clazz.annotationsMap().keySet()) {
            if (annotationNames.contains(annotation)) {
                return true;
            }
        }
        return false;
    }

}

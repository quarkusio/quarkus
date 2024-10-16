package io.quarkus.arc.deployment;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;
import io.quarkus.deployment.builditem.TestProfileBuildItem;

public class TestsAsBeansProcessor {

    @BuildStep
    public void testAnnotations(List<TestAnnotationBuildItem> items, BuildProducer<BeanDefiningAnnotationBuildItem> producer) {
        for (TestAnnotationBuildItem item : items) {
            producer.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(item.getAnnotationClassName())));
        }
    }

    @BuildStep
    public void testClassBeans(List<TestClassBeanBuildItem> items, BuildProducer<AdditionalBeanBuildItem> producer) {
        if (items.isEmpty()) {
            return;
        }

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        for (TestClassBeanBuildItem item : items) {
            builder.addBeanClass(item.getTestClassName());
        }
        producer.produce(builder.build());
    }

    @BuildStep(onlyIf = IsTest.class)
    AnnotationsTransformerBuildItem vetoTestProfileBeans(Optional<TestProfileBuildItem> testProfile,
            CustomScopeAnnotationsBuildItem customScopes, CombinedIndexBuildItem index) {
        if (index.getIndex().getAllKnownImplementors(QUARKUS_TEST_PROFILE).isEmpty()) {
            // No test profiles found
            return null;
        }

        Set<DotName> currentTestProfileHierarchy = initTestProfileHierarchy(testProfile, index.getComputingIndex());
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public void transform(TransformationContext context) {
                AnnotationTarget target = context.getTarget();
                if (target.kind() == Kind.METHOD) {
                    vetoProducerIfNecessary(target.asMethod().declaringClass(), context);
                } else if (target.kind() == Kind.FIELD) {
                    vetoProducerIfNecessary(target.asField().declaringClass(), context);
                } else if (target.kind() == Kind.CLASS) {
                    ClassInfo clazz = target.asClass();
                    if (clazz.nestingType() == NestingType.INNER && Modifier.isStatic(clazz.flags())) {
                        ClassInfo enclosing = index.getComputingIndex().getClassByName(clazz.enclosingClass());
                        if (customScopes.isScopeIn(context.getAnnotations())
                                && isTestProfileClass(enclosing, index.getComputingIndex())
                                && !currentTestProfileHierarchy.contains(enclosing.name())) {
                            // Veto static nested class declared on a test profile class
                            context.transform().add(DotNames.VETOED).done();
                        }
                    }
                }
            }

            private void vetoProducerIfNecessary(ClassInfo declaringClass, TransformationContext context) {
                if (Annotations.contains(context.getAnnotations(), DotNames.PRODUCES)
                        && isTestProfileClass(declaringClass, index.getComputingIndex())
                        && !currentTestProfileHierarchy.contains(declaringClass.name())) {
                    // Veto producer method/field declared on a test profile class
                    context.transform().add(DotNames.VETOED_PRODUCER).done();
                }
            }
        });
    }

    private static final DotName QUARKUS_TEST_PROFILE = DotName.createSimple("io.quarkus.test.junit.QuarkusTestProfile");

    private static Set<DotName> initTestProfileHierarchy(Optional<TestProfileBuildItem> testProfile, IndexView index) {
        Set<DotName> ret = Set.of();
        if (testProfile.isPresent()) {
            DotName testProfileClassName = DotName.createSimple(testProfile.get().getTestProfileClassName());
            ret = Set.of(testProfileClassName);
            ClassInfo testProfileClass = index.getClassByName(testProfile.get().getTestProfileClassName());
            if (testProfileClass != null && !testProfileClass.superName().equals(DotName.OBJECT_NAME)) {
                ret = new HashSet<>();
                ret.add(testProfileClassName);
                DotName superName = testProfileClass.superName();
                while (superName != null && !superName.equals(DotNames.OBJECT)) {
                    ret.add(superName);
                    ClassInfo superClass = index.getClassByName(superName);
                    if (superClass != null) {
                        superName = superClass.superName();
                    } else {
                        superName = null;
                    }
                }
            }
        }
        return ret;
    }

    private static boolean isTestProfileClass(ClassInfo clazz, IndexView index) {
        if (clazz.interfaceNames().contains(QUARKUS_TEST_PROFILE)) {
            return true;
        }
        DotName superName = clazz.superName();
        while (superName != null && !superName.equals(DotNames.OBJECT)) {
            ClassInfo superClass = index.getClassByName(superName);
            if (superClass != null) {
                if (superClass.interfaceNames().contains(QUARKUS_TEST_PROFILE)) {
                    return true;
                }
                superName = superClass.superName();
            } else {
                superName = null;
            }
        }
        return false;
    }

}

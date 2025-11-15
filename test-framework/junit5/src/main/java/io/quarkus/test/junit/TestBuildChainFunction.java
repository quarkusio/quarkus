package io.quarkus.test.junit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.builditem.TestProfileBuildItem;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.TestClassIndexer;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;

public class TestBuildChainFunction implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

    private static final String QUARKUS_TEST_NAME = QuarkusTest.class.getName();
    private static final String QUARKUS_TEST_EXTENSION_NAME = QuarkusTestExtension.class.getName();

    @Override
    public List<Consumer<BuildChainBuilder>> apply(Map<String, Object> stringObjectMap) {
        Path testLocation = (Path) stringObjectMap.get(AbstractJvmQuarkusTestExtension.TEST_LOCATION);
        // the index was written by the extension
        Index testClassesIndex = TestClassIndexer.readIndex(testLocation,
                (Class<?>) stringObjectMap.get(AbstractJvmQuarkusTestExtension.TEST_CLASS));
        Collection<TestAnnotationBuildItem> testAnnotationBuildItems = collectTestAnnotationItems(testClassesIndex);

        List<Consumer<BuildChainBuilder>> allCustomizers = new ArrayList<>(1);
        Consumer<BuildChainBuilder> defaultCustomizer = new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder buildChainBuilder) {
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new TestClassPredicateBuildItem(new Predicate<String>() {
                            @Override
                            public boolean test(String className) {
                                return PathTestHelper.isTestClass(className,
                                        Thread.currentThread()
                                                .getContextClassLoader(),
                                        testLocation);
                            }
                        }));
                    }
                })
                        .produces(TestClassPredicateBuildItem.class)
                        .build();
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        //we need to make sure all hot reloadable classes are application classes
                        context.produce(new ApplicationClassPredicateBuildItem(new Predicate<String>() {
                            @Override
                            public boolean test(String className) {
                                return QuarkusClassLoader.isApplicationClass(className);
                            }
                        }));
                    }
                })
                        .produces(ApplicationClassPredicateBuildItem.class)
                        .build();
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        testAnnotationBuildItems.forEach(context::produce);
                    }
                })
                        .produces(TestAnnotationBuildItem.class)
                        .build();

                List<String> testClassBeans = new ArrayList<>();

                List<AnnotationInstance> extendWith = testClassesIndex
                        .getAnnotations(DotNames.EXTEND_WITH);
                for (AnnotationInstance annotationInstance : extendWith) {
                    if (annotationInstance.target()
                            .kind() != AnnotationTarget.Kind.CLASS) {
                        continue;
                    }
                    ClassInfo classInfo = annotationInstance.target()
                            .asClass();
                    if (classInfo.isAnnotation()) {
                        continue;
                    }
                    Type[] extendsWithTypes = annotationInstance.value()
                            .asClassArray();
                    for (Type type : extendsWithTypes) {
                        if (DotNames.QUARKUS_TEST_EXTENSION.equals(type.name())) {
                            testClassBeans.add(classInfo.name()
                                    .toString());
                        }
                    }
                }

                List<AnnotationInstance> registerExtension = testClassesIndex.getAnnotations(DotNames.REGISTER_EXTENSION);
                for (AnnotationInstance annotationInstance : registerExtension) {
                    if (annotationInstance.target()
                            .kind() != AnnotationTarget.Kind.FIELD) {
                        continue;
                    }
                    FieldInfo fieldInfo = annotationInstance.target()
                            .asField();
                    if (DotNames.QUARKUS_TEST_EXTENSION.equals(fieldInfo.type()
                            .name())) {
                        testClassBeans.add(fieldInfo.declaringClass()
                                .name()
                                .toString());
                    }
                }

                if (!testClassBeans.isEmpty()) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            for (String quarkusExtendWithTestClass : testClassBeans) {
                                context.produce(new TestClassBeanBuildItem(quarkusExtendWithTestClass));
                            }
                        }
                    })
                            .produces(TestClassBeanBuildItem.class)
                            .build();
                }

                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        Object testProfile = stringObjectMap.get(AbstractJvmQuarkusTestExtension.TEST_PROFILE);
                        if (testProfile != null) {
                            context.produce(new TestProfileBuildItem(testProfile.toString()));
                        }
                    }
                })
                        .produces(TestProfileBuildItem.class)
                        .build();

            }
        };
        allCustomizers.add(defaultCustomizer);

        // give other extensions the ability to customize the build chain
        for (TestBuildChainCustomizerProducer testBuildChainCustomizerProducer : ServiceLoader
                .load(TestBuildChainCustomizerProducer.class, this.getClass()
                        .getClassLoader())) {
            allCustomizers.add(testBuildChainCustomizerProducer.produce(testClassesIndex));
        }

        return allCustomizers;
    }

    private static Collection<TestAnnotationBuildItem> collectTestAnnotationItems(Index testClassesIndex) {
        var result = new HashSet<String>();
        result.add(QUARKUS_TEST_NAME);

        // collect meta-annotations with @QuarkusTest
        result.addAll(collectMetaAnnotations(QUARKUS_TEST_NAME, testClassesIndex));
        // collect meta-annotations with @ExtendWith(QuarkusTestExtension.class)
        Predicate<AnnotationInstance> isValueQuarkusTestExtension = ai -> ai.value() != null
                && Arrays.stream(ai.value().asClassArray())
                        .anyMatch(type -> type.name() != null && QUARKUS_TEST_EXTENSION_NAME.equals(type.name().toString()));
        result.addAll(collectMetaAnnotations(ExtendWith.class.getName(), testClassesIndex, isValueQuarkusTestExtension));

        return result.stream().map(TestAnnotationBuildItem::new).toList();
    }

    private static List<String> collectMetaAnnotations(String annotationName, Index testClassesIndex) {
        return collectMetaAnnotations(annotationName, testClassesIndex, ai -> true);
    }

    private static List<String> collectMetaAnnotations(String annotationName, Index testClassesIndex,
            Predicate<AnnotationInstance> initialFilter) {
        return testClassesIndex.getAnnotations(annotationName)
                .stream()
                .filter(initialFilter)
                .map(AnnotationInstance::target)
                .filter(Objects::nonNull)
                .filter(at -> at.kind() == AnnotationTarget.Kind.CLASS)
                .map(AnnotationTarget::asClass)
                .filter(ClassInfo::isAnnotation)
                .<String> mapMulti((ci, consumer) -> {
                    consumer.accept(ci.name().toString());
                    collectMetaAnnotations(ci.name().toString(), testClassesIndex).forEach(consumer);
                })
                .toList();
    }
}

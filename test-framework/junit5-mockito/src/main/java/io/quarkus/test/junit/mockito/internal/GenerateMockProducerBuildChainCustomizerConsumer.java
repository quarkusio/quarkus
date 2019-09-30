package io.quarkus.test.junit.mockito.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.test.junit.chain.TestBuildChainCustomizerConsumer;
import io.quarkus.test.junit.mockito.MockBean;

public class GenerateMockProducerBuildChainCustomizerConsumer implements TestBuildChainCustomizerConsumer {

    @Override
    public void accept(BuildChainBuilder buildChainBuilder) {
        buildChainBuilder.addBuildStep(new GenerateProducerBuildStep())
                .consumes(CombinedIndexBuildItem.class)
                .produces(GeneratedBeanBuildItem.class)
                .build();
    }

    private static class GenerateProducerBuildStep implements BuildStep {

        private static final DotName MOCK_BEAN = DotName.createSimple(MockBean.class.getName());
        private static final DotName GENERATED_PRODUCER = DotName.createSimple("io.quarkus.test.junit.mockito.MockProducer");
        private static final String MOCKITO_CLASS_NAME = "org.mockito.Mockito";

        @Override
        public void execute(BuildContext context) {
            CombinedIndexBuildItem combinedIndex = context.consume(CombinedIndexBuildItem.class);
            IndexView index = combinedIndex.getIndex();
            Collection<AnnotationInstance> annotationInstances = index.getAnnotations(MOCK_BEAN);
            if (annotationInstances.isEmpty()) {
                return;
            }

            ClassOutput classOutput = new ClassOutput() {
                @Override
                public void write(String name, byte[] data) {
                    context.produce(new GeneratedBeanBuildItem(name, data));
                }
            };

            List<Type> typesToMock = new ArrayList<>(annotationInstances.size());
            for (AnnotationInstance annotationInstance : annotationInstances) {
                if (annotationInstance.target().kind() != AnnotationTarget.Kind.FIELD) {
                    throw new IllegalArgumentException(
                            "@MockBean can only be used on fields. Offending instance is " + annotationInstance.target());
                }
                FieldInfo fieldInfo = annotationInstance.target().asField();
                typesToMock.add(fieldInfo.type());

                MocksTracker.track(fieldInfo.declaringClass().name().toString(), fieldInfo.type().name().toString());
            }

            generateProducer(classOutput, typesToMock);
        }

        private void generateProducer(ClassOutput classOutput, Collection<Type> typesToMock) {
            try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                    .className(GENERATED_PRODUCER.toString()).build()) {
                classCreator.addAnnotation(ApplicationScoped.class);

                for (Type typeToMock : typesToMock) {
                    DotName classToMock = typeToMock.name();
                    String simpleName = classToMock.isInner() ? classToMock.local() : classToMock.withoutPackagePrefix();

                    String methodName = "produce_" + simpleName + "_" + HashUtil.sha1(classToMock.toString());
                    try (MethodCreator methodCreator = classCreator.getMethodCreator(methodName, classToMock.toString())) {
                        methodCreator.addAnnotation(Produces.class);
                        methodCreator.addAnnotation(Singleton.class);
                        addAlternativePriorityAnnotation(classToMock, methodName, methodCreator);

                        ResultHandle mock = methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(MOCKITO_CLASS_NAME, "mock", Object.class.getName(),
                                        Class.class.getName()),
                                methodCreator.loadClass(classToMock.toString()));

                        methodCreator.returnValue(mock);
                    }
                }
            }
        }

        private void addAlternativePriorityAnnotation(DotName classToMock, String methodName, MethodCreator methodCreator) {
            methodCreator.addAnnotation(
                    AnnotationInstance.create(DotNames.ALTERNATIVE_PRIORITY,
                            MethodInfo.create(
                                    ClassInfo.create(GENERATED_PRODUCER, DotNames.OBJECT, (short) 0, new DotName[] {},
                                            Collections.emptyMap(), true),
                                    methodName, new Type[] {}, Type.create(classToMock, Type.Kind.CLASS), (short) 0),
                            Collections.singletonList(AnnotationValue.createIntegerValue("value", 1))));
        }
    }
}

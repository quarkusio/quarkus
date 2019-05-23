package io.quarkus.arc.test.build.extension.injectionPoints;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Qualifier;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class InjectionPointTransformerTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(SimpleProducer.class, SimpleConsumer.class, MyQualifier.class, CasualObserver.class,
                    AnotherQualifier.class)
            .injectionPointsTransformers(new MyTransformer())
            .build();

    @Test
    public void testQualifierWasAddedToInjectionPoint() {
        ArcContainer arc = Arc.container();
        Assert.assertTrue(arc.instance(SimpleConsumer.class).isAvailable());
        SimpleConsumer bean = arc.instance(SimpleConsumer.class).get();
        Assert.assertEquals("foo", bean.getFoo());
        Assert.assertEquals("bar", bean.getBar());

        Assert.assertTrue(arc.instance(CasualObserver.class).isAvailable());
        Arc.container().beanManager().getEvent().select(Integer.class).fire(42);
        CasualObserver observer = arc.instance(CasualObserver.class).get();
        Assert.assertEquals("bar", observer.getChangedString());
        Assert.assertEquals("foo", observer.getUnchangedString());
    }

    @ApplicationScoped
    static class SimpleProducer {

        @Produces
        @MyQualifier
        @Dependent
        String producedString = "foo";

        @Produces
        @Dependent
        String producedStringNoQualifier = "bar";

    }

    @ApplicationScoped
    static class SimpleConsumer {

        @Inject
        // We will add @MyQualifier here
        String foo;

        @Inject
        // Nothing should be added here
        String bar;

        public String getBar() {
            return bar;
        }

        public String getFoo() {
            return foo;
        }
    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface AnotherQualifier {

    }

    @ApplicationScoped
    static class CasualObserver {

        String changeMe;
        String dontChangeMe;

        // there is no String with qualifier @AnotherQualifier, we will remove it
        public void observe(@Observes Integer payload, @AnotherQualifier String changeMe, @MyQualifier String dontChangeMe) {
            this.changeMe = changeMe;
            this.dontChangeMe = dontChangeMe;
        }

        public String getChangedString() {
            return changeMe;
        }

        public String getUnchangedString() {
            return dontChangeMe;
        }
    }

    static class MyTransformer implements InjectionPointsTransformer {

        @Override
        public boolean appliesTo(Type requiredType) {
            // applies to all String injection points
            return requiredType.equals(Type.create(DotName.createSimple(String.class.getName()), Type.Kind.CLASS));
        }

        @Override
        public void transform(TransformationContext transformationContext) {
            AnnotationTarget.Kind kind = transformationContext.getTarget().kind();
            if (AnnotationTarget.Kind.FIELD.equals(transformationContext.getTarget().kind())) {
                FieldInfo fieldInfo = transformationContext.getTarget().asField();
                // with this we should be able to filter out only fields we want to affect
                if (fieldInfo.declaringClass().name().equals(DotName.createSimple(SimpleConsumer.class.getName()))
                        && fieldInfo.name().equals("foo")) {
                    transformationContext.transform().add(MyQualifier.class).done();
                }

            } else if (AnnotationTarget.Kind.METHOD.equals(transformationContext.getTarget().kind())) {
                MethodInfo methodInfo = transformationContext.getTarget().asMethod();
                DotName anotherQualifierDotName = DotName.createSimple(AnotherQualifier.class.getName());
                if (methodInfo.declaringClass().name()
                        .equals(DotName.createSimple(CasualObserver.class.getName()))
                        && transformationContext.getAllAnnotations().stream()
                                .anyMatch(p -> p.name().equals(anotherQualifierDotName))) {
                    transformationContext.transform()
                            .remove(annotationInstance -> annotationInstance.name().equals(anotherQualifierDotName))
                            .done();
                }
            } else {
                throw new IllegalStateException("Unexpected injection point kind: " + transformationContext.getTarget().kind());
            }
        }
    }
}

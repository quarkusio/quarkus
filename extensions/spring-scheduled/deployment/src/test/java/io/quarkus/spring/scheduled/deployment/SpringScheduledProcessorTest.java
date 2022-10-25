package io.quarkus.spring.scheduled.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.arc.processor.BeanArchives;
import io.quarkus.deployment.util.IoUtil;

public class SpringScheduledProcessorTest {

    final SpringScheduledProcessor springScheduledProcessor = new SpringScheduledProcessor();
    final IndexView index = getIndex(SpringScheduledMethodsBean.class);

    @Test
    public void testBuildCronParam() {

        final MethodInfo target = index.getClassByName(DotName.createSimple(SpringScheduledMethodsBean.class.getName()))
                .method("checkEverySecondCron");
        AnnotationInstance annotation = target.annotation(SpringScheduledProcessor.SPRING_SCHEDULED);
        AnnotationValue annotationValue = springScheduledProcessor.buildCronParam(annotation.values());
        assertThat(annotationValue.name()).isEqualTo("cron");
        assertThat(annotationValue.value()).isEqualTo("0/1 * * * * ?");

    }

    @Test
    public void testBuildEveryParam() {

        final MethodInfo target = index.getClassByName(DotName.createSimple(SpringScheduledMethodsBean.class.getName()))
                .method("checkEverySecond");
        AnnotationInstance annotation = target.annotation(SpringScheduledProcessor.SPRING_SCHEDULED);
        AnnotationValue annotationValue = springScheduledProcessor.buildEveryParam(annotation.values());
        assertThat(annotationValue.name()).isEqualTo("every");
        assertThat(annotationValue.value()).isEqualTo("PT1S");

    }

    @Test
    public void testBuildDelayParam() {

        final MethodInfo target = index.getClassByName(DotName.createSimple(BeanWithScheduledMethods.class.getName()))
                .method("checkEverySecondWithDelay");
        AnnotationInstance annotation = target.annotation(SpringScheduledProcessor.SPRING_SCHEDULED);
        List<AnnotationValue> annotationValues = springScheduledProcessor.buildDelayParams(annotation.values());
        AnnotationValue expectedDelayValue = AnnotationValue.createLongValue("delay", 1000L);
        AnnotationValue expectedDelayUnitValue = AnnotationValue.createEnumValue("delayUnit",
                DotName.createSimple("java.util.concurrent.TimeUnit"),
                TimeUnit.MILLISECONDS.name());
        assertThat(annotationValues).contains(expectedDelayValue);
        assertThat(annotationValues).contains(expectedDelayUnitValue);

    }

    @Test
    public void testBuildDelayParamFromString() {

        final MethodInfo target = index.getClassByName(DotName.createSimple(BeanWithScheduledMethods.class.getName()))
                .method("checkEverySecondWithDelayString");
        AnnotationInstance annotation = target.annotation(SpringScheduledProcessor.SPRING_SCHEDULED);
        List<AnnotationValue> annotationValues = springScheduledProcessor.buildDelayParams(annotation.values());
        AnnotationValue expectedDelayValue = AnnotationValue.createLongValue("delay", 1000L);
        AnnotationValue expectedDelayUnitValue = AnnotationValue.createEnumValue("delayUnit",
                DotName.createSimple("java.util.concurrent.TimeUnit"),
                TimeUnit.MILLISECONDS.name());
        assertThat(annotationValues).contains(expectedDelayValue);
        assertThat(annotationValues).contains(expectedDelayUnitValue);

    }

    @Test
    public void testBuildEveryParamFromInvalidFormat() {

        final MethodInfo target = index.getClassByName(DotName.createSimple(BeanWithScheduledMethods.class.getName()))
                .method("invalidFormatFixedRate");
        AnnotationInstance annotation = target.annotation(SpringScheduledProcessor.SPRING_SCHEDULED);
        try {
            springScheduledProcessor.buildEveryParam(annotation.values());
            fail();
        } catch (IllegalArgumentException expected) {

        }

    }

    @Test
    public void testBuildDelayParamFromInvalidFormat() {

        final MethodInfo target = index.getClassByName(DotName.createSimple(BeanWithScheduledMethods.class.getName()))
                .method("invalidFormatInitialDelay");
        AnnotationInstance annotation = target.annotation(SpringScheduledProcessor.SPRING_SCHEDULED);
        try {
            springScheduledProcessor.buildDelayParams(annotation.values());
            fail();
        } catch (IllegalArgumentException expected) {

        }

    }

    private IndexView getIndex(final Class<?>... classes) {
        final Indexer indexer = new Indexer();
        for (final Class<?> clazz : classes) {
            final String className = clazz.getName();
            try (InputStream stream = IoUtil.readClass(getClass().getClassLoader(), className)) {
                final ClassInfo beanInfo = indexer.index(stream);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to index: " + className, e);
            }
        }
        return BeanArchives.buildBeanArchiveIndex(getClass().getClassLoader(), new ConcurrentHashMap<>(),
                indexer.complete());
    }

    @ApplicationScoped
    static class BeanWithScheduledMethods {

        @Scheduled(fixedRate = 1000, initialDelay = 1000)
        void checkEverySecondWithDelay() {

        }

        @Scheduled(fixedRateString = "1000", initialDelayString = "1000")
        void checkEverySecondWithDelayString() {

        }

        @Scheduled(fixedRateString = "1000", initialDelayString = "invalid format")
        void invalidFormatInitialDelay() {

        }

        @Scheduled(fixedRateString = "invalid format")
        void invalidFormatFixedRate() {
        }

    }
}

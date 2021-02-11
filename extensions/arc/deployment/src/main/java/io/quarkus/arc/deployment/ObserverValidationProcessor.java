package io.quarkus.arc.deployment;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;

/**
 * Validates observer methods from application classes.
 * If an observer listening for {@code @Initialized(ApplicationScoped.class)} is found, it logs a warning.
 */
public class ObserverValidationProcessor {

    private static final Logger LOGGER = Logger.getLogger(ObserverValidationProcessor.class.getName());

    @BuildStep
    public void validateApplicationObserver(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<BeanDeploymentValidatorBuildItem> validators) {
        // an index of all root archive classes (usually src/main/classes)
        IndexView applicationClassesIndex = applicationArchivesBuildItem.getRootArchive().getIndex();

        validators.produce(new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

            @Override
            public void validate(ValidationContext context) {
                Collection<ObserverInfo> allObservers = context.get(Key.OBSERVERS);
                // do the validation for each observer that can be found within application classes
                for (ObserverInfo observer : allObservers) {
                    if (observer.isSynthetic()) {
                        // Skip synthetic observers
                        continue;
                    }
                    DotName declaringBeanDotName = observer.getDeclaringBean().getBeanClass();
                    AnnotationInstance instance = Annotations.getParameterAnnotation(observer.getObserverMethod(),
                            DotNames.INITIALIZED);
                    if (applicationClassesIndex.getClassByName(declaringBeanDotName) != null && instance != null &&
                            instance.value().asClass().name().equals(BuiltinScope.APPLICATION.getName())) {
                        // found an observer for @Initialized(ApplicationScoped.class)
                        // log a warning and recommend to use StartupEvent instead
                        final String observerWarning = "The method %s#%s is an observer for " +
                                "@Initialized(ApplicationScoped.class). Observer notification for this event may " +
                                "vary between JVM and native modes! We strongly recommend to observe StartupEvent " +
                                "instead as that one is consistently delivered in both modes once the container is " +
                                "running.";
                        LOGGER.warnf(observerWarning, observer.getDeclaringBean().getImplClazz(),
                                observer.getObserverMethod().name());
                    }
                }
            }
        }));
    }
}

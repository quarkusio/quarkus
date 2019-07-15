package io.quarkus.axon.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.axon.runtime.AxonClientProducer;
import io.quarkus.axon.runtime.AxonRuntimeConfig;
import io.quarkus.axon.runtime.AxonRuntimeTemplate;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.modelling.command.AggregateRoot;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryHandler;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

public class AxonProcessor {

    private static final Logger LOGGER = Logger.getLogger(AxonProcessor.class);

    private static DotName AGGREGATE_ANNOTATION = DotName.createSimple(AggregateRoot.class.getName());
    private static DotName SAGA_ANNOTATION = DotName.createSimple(StartSaga.class.getName());
    private static DotName COMMAND_HANDLER_ANNOTATION = DotName.createSimple(CommandHandler.class.getName());
    private static DotName EVENT_HANDLER_ANNOTATION = DotName.createSimple(EventHandler.class.getName());
    private static DotName QUERY_HANDLER_ANNOTATION = DotName.createSimple(QueryHandler.class.getName());

    @BuildStep
    @Record(STATIC_INIT)
    public void build(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            BuildProducer<FeatureBuildItem> featureProducer) {

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.AXON));

        additionalBeanProducer.produce(AdditionalBeanBuildItem.unremovableOf(AxonClientProducer.class));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void scanForAggregates(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<AggregateBuildItem> axonBuildItemProducer) {
        scanForBeans(beanArchiveIndex, axonBuildItemProducer, AGGREGATE_ANNOTATION, AggregateBuildItem::new);
    }

    @BuildStep
    @Record(STATIC_INIT)
    void scanForSagas(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<SagaBuildItem> axonBuildItemProducer) {
        scanForBeans(beanArchiveIndex, axonBuildItemProducer, SAGA_ANNOTATION, SagaBuildItem::new);
    }

    @BuildStep
    @Record(STATIC_INIT)
    void scanForCommandHandlers(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<CommandHandlerBuildItem> axonBuildItemProducer) {
        scanForBeans(beanArchiveIndex, axonBuildItemProducer, COMMAND_HANDLER_ANNOTATION, CommandHandlerBuildItem::new);
    }

    @BuildStep
    @Record(STATIC_INIT)
    void scanForEventHandlers(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<EventHandlerBuildItem> axonBuildItemProducer) {
        scanForBeans(beanArchiveIndex, axonBuildItemProducer, EVENT_HANDLER_ANNOTATION, EventHandlerBuildItem::new);
    }

    @BuildStep
    @Record(STATIC_INIT)
    void scanForQueryHandlers(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<QueryHandlerBuildItem> axonBuildItemProducer) {
        scanForBeans(beanArchiveIndex, axonBuildItemProducer, QUERY_HANDLER_ANNOTATION, QueryHandlerBuildItem::new);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void injectBeanContainerIntoBeanResolverFactory(AxonRuntimeTemplate template, BeanContainerBuildItem beanContainer) {
        template.injectBeanContainerIntoBeanResolverFactory(beanContainer.getValue());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void configureRuntimeProperties(AxonRuntimeTemplate template, AxonRuntimeConfig axonRuntimeConfig,
            BeanContainerBuildItem beanContainer) {
        template.setAxonBuildConfig(beanContainer.getValue(), axonRuntimeConfig);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerSagas(AxonRuntimeTemplate template, List<SagaBuildItem> axonAnnotatedBeans,
            BeanContainerBuildItem beanContainer) {
        axonAnnotatedBeans.forEach(item -> template.registerSaga(beanContainer.getValue(), item.getAxonAnnotatedClass()));

    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerAggregates(AxonRuntimeTemplate template, List<AggregateBuildItem> axonAnnotatedBeans,
            BeanContainerBuildItem beanContainer) {
        axonAnnotatedBeans.forEach(item -> template.registerAggregate(beanContainer.getValue(), item.getAxonAnnotatedClass()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerCommandHandlers(AxonRuntimeTemplate template, List<CommandHandlerBuildItem> axonAnnotatedBeans,
            BeanContainerBuildItem beanContainer) {
        axonAnnotatedBeans
                .forEach(item -> template.registerCommandHandler(beanContainer.getValue(), item.getAxonAnnotatedClass()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerEventHandlers(AxonRuntimeTemplate template, List<EventHandlerBuildItem> axonAnnotatedBeans,
            BeanContainerBuildItem beanContainer) {
        axonAnnotatedBeans
                .forEach(item -> template.registerEventHandler(beanContainer.getValue(), item.getAxonAnnotatedClass()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void registerQueryHandlers(AxonRuntimeTemplate template, List<QueryHandlerBuildItem> axonAnnotatedBeans,
            BeanContainerBuildItem beanContainer) {
        axonAnnotatedBeans
                .forEach(item -> template.registerQueryHandler(beanContainer.getValue(), item.getAxonAnnotatedClass()));
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void initializeAxon(AxonRuntimeTemplate template, BeanContainerBuildItem beanContainer) {
        template.initializeAxonClient(beanContainer.getValue());
    }

    /**
     * This method scans for beans with the annotation type on class, field or method level and returns the classname
     */
    private <T extends AxonBuildItem> void scanForBeans(BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<T> axonBuildItemProducer,
            DotName annotationType,
            Function<Class<?>, T> buildItem) {

        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> testBeans = indexView.getAnnotations(annotationType);
        for (AnnotationInstance ann : testBeans) {
            try {
                ClassInfo beanClassInfo;
                if (AnnotationTarget.Kind.CLASS.equals(ann.target().kind())) {
                    beanClassInfo = ann.target().asClass();
                } else {
                    beanClassInfo = ann.target().asMethod().declaringClass();
                }

                Class<?> beanClass = Class.forName(beanClassInfo.name().toString());
                axonBuildItemProducer.produce(buildItem.apply(beanClass));

                LOGGER.debug("Found " + annotationType.toString() + " annotation on class " + beanClass
                        + ". Item will be registered to Axon.");
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Failed to load bean class", e);
            }
        }
    }

}

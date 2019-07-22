package io.quarkus.axon.runtime;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.axonserver.connector.command.AxonServerCommandBus;
import org.axonframework.axonserver.connector.event.axon.AxonServerEventStore;
import org.axonframework.axonserver.connector.query.AxonServerQueryBus;
import org.axonframework.axonserver.connector.query.QueryPriorityCalculator;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.distributed.AnnotationRoutingStrategy;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.queryhandling.*;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.NoOpEventUpcaster;
import org.axonframework.serialization.xml.XStreamSerializer;

@ApplicationScoped
public class AxonClientProducer {

    private AxonRuntimeConfig axonRuntimeConfig;

    private List<Object> sagas = new ArrayList<>();
    private List<Object> aggregates = new ArrayList<>();
    private List<Object> commandHandlers = new ArrayList<>();
    private List<Object> eventHandlers = new ArrayList<>();
    private List<Object> queryHandlers = new ArrayList<>();

    void setAxonRuntimeConfig(AxonRuntimeConfig axonRuntimeConfig) {
        this.axonRuntimeConfig = axonRuntimeConfig;
    }

    @Produces
    @ApplicationScoped
    public Configuration initializeConfiguration(Configurer configurer) {
        Configuration configuration = configurer.buildConfiguration();
        if (axonRuntimeConfig.autostart) {
            configuration.start();
        }
        return configuration;
    }

    @Produces
    @Dependent
    public Configurer quarkusDefaultConfigurer(EventBus eventBus, CommandBus commandBus, QueryBus queryBus,
            Serializer serializer) {

        Configurer configurer = DefaultConfigurer.defaultConfiguration(false)
                .configureEventBus(c -> eventBus)
                .configureCommandBus(c -> commandBus)
                .configureQueryBus(c -> queryBus)
                .configureSerializer(c -> serializer);

        // When registering a Saga or Aggregate also the EventSourcingHandlers inside
        // of that class are registered
        sagas.forEach(saga -> configurer.eventProcessing().registerSaga(saga.getClass()));
        aggregates.forEach(aggregate -> configurer.configureAggregate(aggregate.getClass()));

        // Command handlers, Event handlers and Query handlers must be registered by itself.
        commandHandlers.forEach(commandHandler -> configurer.registerCommandHandler(conf -> commandHandler));
        eventHandlers.forEach(eventHandler -> configurer.registerEventHandler(conf -> eventHandler));
        queryHandlers.forEach(queryHandler -> configurer.registerQueryHandler(conf -> queryHandler));

        return configurer;
    }

    @Produces
    @Dependent
    public AxonServerConfiguration axonServerConfiguration() {
        AxonServerConfiguration.Builder builder = AxonServerConfiguration.builder();
        axonRuntimeConfig.clientId.ifPresent(builder::clientId);
        axonRuntimeConfig.componentName.ifPresent(builder::componentName);

        axonRuntimeConfig.servers.ifPresent(builder::servers);
        if (axonRuntimeConfig.sslEnabled) {
            axonRuntimeConfig.certFile.ifPresent(builder::ssl);
        }

        axonRuntimeConfig.token.ifPresent(builder::token);
        axonRuntimeConfig.eventSecretKey.ifPresent(builder::setEventSecretKey);

        axonRuntimeConfig.context.ifPresent(builder::context);
        axonRuntimeConfig.maxMessageSize.ifPresent(builder::maxMessageSize);
        axonRuntimeConfig.snapshotPrefetch.ifPresent(builder::snapshotPrefetch);

        return builder.build();
    }

    @Produces
    @Dependent
    public AxonServerConnectionManager axonServerConnectionManager(AxonServerConfiguration axonServerConfiguration) {
        return new AxonServerConnectionManager(axonServerConfiguration);
    }

    @Produces
    @Dependent
    public EventBus eventBus(Serializer serializer, AxonServerConfiguration axonServerConfiguration,
            AxonServerConnectionManager axonServerConnectionManager) {

        return AxonServerEventStore.builder()
                .eventSerializer(serializer)
                .snapshotSerializer(serializer)
                .configuration(axonServerConfiguration)
                .platformConnectionManager(axonServerConnectionManager)
                .upcasterChain(NoOpEventUpcaster.INSTANCE)
                .build();
    }

    @Produces
    @Dependent
    public CommandBus commandBus(AxonServerConnectionManager axonServerConnectionManager,
            AxonServerConfiguration axonServerConfiguration, Serializer serializer) {

        return new AxonServerCommandBus(axonServerConnectionManager, axonServerConfiguration,
                SimpleCommandBus.builder().build(),
                serializer, new AnnotationRoutingStrategy());
    }

    @Produces
    @Dependent
    public QueryBus queryBus(AxonServerConnectionManager axonServerConnectionManager,
            AxonServerConfiguration axonServerConfiguration,
            Serializer serializer) {

        return new AxonServerQueryBus(axonServerConnectionManager, axonServerConfiguration,
                SimpleQueryUpdateEmitter.builder().build(), SimpleQueryBus.builder().build(), serializer, serializer,
                new QueryPriorityCalculator() {
                    @Override
                    public int determinePriority(QueryMessage<?, ?> queryMessage) {
                        return 0;
                    }
                });
    }

    @Produces
    @Dependent
    public QueryGateway queryGateway(QueryBus queryBus) {
        return DefaultQueryGateway.builder().queryBus(queryBus).build();
    }

    @Produces
    @Dependent
    public CommandGateway commandGateway(CommandBus commandBus) {
        return DefaultCommandGateway.builder().commandBus(commandBus).build();
    }

    @Produces
    @Dependent
    public Serializer serializer() {
        return XStreamSerializer.builder().build();
    }

    void registerSaga(Object annotatedBean) {
        addIfNotExist(annotatedBean, sagas);
    }

    void registerAggregate(Object annotatedBean) {
        addIfNotExist(annotatedBean, aggregates);
    }

    void registerEventHandler(Object annotatedBean) {
        addIfNotExist(annotatedBean, eventHandlers);
    }

    void registerCommandHandler(Object annotatedBean) {
        addIfNotExist(annotatedBean, commandHandlers);
    }

    void registerQueryHandler(Object annotatedBean) {
        addIfNotExist(annotatedBean, queryHandlers);
    }

    private void addIfNotExist(Object annotatedBean, List<Object> aggregates) {
        boolean alreadyExist = aggregates.stream().anyMatch(i -> i.getClass().equals(annotatedBean.getClass()));
        if (!alreadyExist) {
            aggregates.add(annotatedBean);
        }
    }
}

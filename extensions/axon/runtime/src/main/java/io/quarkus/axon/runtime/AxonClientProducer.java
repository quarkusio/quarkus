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
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.upcasting.event.NoOpEventUpcaster;

@ApplicationScoped
public class AxonClientProducer {

    private Configuration configuration;
    private Configurer configurer;
    private AxonRuntimeConfig axonRuntimeConfig;
    private AxonServerConnectionManager axonServerConnectionManager;
    private AxonServerConfiguration axonServerConfiguration;
    private EventBus eventBus;
    private CommandBus commandBus;
    private QueryBus queryBus;
    private Serializer serializer;
    private DefaultQueryGateway queryGateway;
    private DefaultCommandGateway commandGateway;

    private List<Object> sagas = new ArrayList<>();
    private List<Object> aggregates = new ArrayList<>();
    private List<Object> commandHandlers = new ArrayList<>();
    private List<Object> eventHandlers = new ArrayList<>();
    private List<Object> queryHandlers = new ArrayList<>();

    void setAxonRuntimeConfig(AxonRuntimeConfig axonRuntimeConfig) {
        this.axonRuntimeConfig = axonRuntimeConfig;
    }

    @Produces
    public Configurer quarkusDefaultConfigurer() {
        if (configurer == null) {
            configurer = DefaultConfigurer.defaultConfiguration()
                    .configureEventBus(c -> eventBus())
                    .configureCommandBus(c -> commandBus())
                    .configureQueryBus(c -> queryBus())
                    .configureSerializer(c -> serializer());

            // When registering a Saga or Aggregate also the EventSourcingHandlers inside
            // of that class are registered
            sagas.forEach(saga -> configurer.eventProcessing().registerSaga(saga.getClass()));
            aggregates.forEach(aggregate -> configurer.configureAggregate(aggregate.getClass()));

            // Command handlers, Event handlers and Query handlers must be registered by itself.
            commandHandlers.forEach(commandHandler -> configurer.registerCommandHandler(conf -> commandHandler));
            eventHandlers.forEach(eventHandler -> configurer.registerEventHandler(conf -> eventHandler));
            queryHandlers.forEach(queryHandler -> configurer.registerQueryHandler(conf -> queryHandler));
        }
        return configurer;
    }

    @Produces
    @ApplicationScoped
    public Configuration initializeConfiguration() {
        if (configuration == null) {
            configuration = quarkusDefaultConfigurer().buildConfiguration();
            if (axonRuntimeConfig.autostart) {
                configuration.start();
            }
        }
        return configuration;
    }

    @Produces
    @Dependent
    public AxonServerConfiguration axonServerConfiguration() {
        if (axonServerConfiguration == null) {

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

            axonServerConfiguration = builder.build();
        }
        return axonServerConfiguration;
    }

    @Produces
    @Dependent
    public AxonServerConnectionManager axonServerConnectionManager() {
        if (axonServerConnectionManager == null) {
            axonServerConnectionManager = new AxonServerConnectionManager(axonServerConfiguration());
        }
        return axonServerConnectionManager;
    }

    @Produces
    @Dependent
    public EventBus eventBus() {
        if (eventBus == null) {
            eventBus = AxonServerEventStore.builder()
                    .eventSerializer(serializer())
                    .snapshotSerializer(serializer())
                    .configuration(axonServerConfiguration())
                    .platformConnectionManager(axonServerConnectionManager())
                    .upcasterChain(NoOpEventUpcaster.INSTANCE)
                    .build();
        }
        return eventBus;
    }

    @Produces
    @Dependent
    public CommandBus commandBus() {
        if (commandBus == null) {
            CommandBus localSegment = SimpleCommandBus.builder().build();
            commandBus = new AxonServerCommandBus(
                    axonServerConnectionManager(),
                    axonServerConfiguration(),
                    localSegment,
                    serializer(),
                    new AnnotationRoutingStrategy());

        }
        return commandBus;
    }

    @Produces
    @Dependent
    public QueryBus queryBus() {
        if (queryBus == null) {
            SimpleQueryBus localQueryBus = SimpleQueryBus.builder().build();
            SimpleQueryUpdateEmitter queryUpdateEmitter = SimpleQueryUpdateEmitter.builder().build();
            queryBus = new AxonServerQueryBus(axonServerConnectionManager(),
                    axonServerConfiguration(),
                    queryUpdateEmitter,
                    localQueryBus,
                    serializer(),
                    serializer(),
                    new QueryPriorityCalculator() {
                        @Override
                        public int determinePriority(QueryMessage<?, ?> queryMessage) {
                            return 0;
                        }
                    });
        }
        return queryBus;
    }

    @Produces
    @Dependent
    public QueryGateway queryGateway() {
        if (queryGateway == null) {
            queryGateway = DefaultQueryGateway
                    .builder()
                    .queryBus(queryBus())
                    .build();
        }
        return queryGateway;
    }

    @Produces
    @Dependent
    public CommandGateway commandGateway() {
        if (commandGateway == null) {
            commandGateway = DefaultCommandGateway
                    .builder()
                    .commandBus(commandBus())
                    .build();
        }
        return commandGateway;
    }

    @Produces
    @Dependent
    public Serializer serializer() {
        if (serializer == null) {
            serializer = JacksonSerializer.builder().build();
        }
        return serializer;
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

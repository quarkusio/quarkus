package io.quarkus.hibernate.search.orm.coordination.outboxpolling;

import java.util.List;

// Workaround for https://hibernate.atlassian.net/browse/HSEARCH-4450
public final class HibernateSearchOutboxPollingClasses {

    private HibernateSearchOutboxPollingClasses() {
    }

    public static final List<String> JPA_MODEL_CLASSES = List.of(
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEvent$Status",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState");

    public static final List<String> AVRO_GENERATED_CLASSES = List.of(
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.DocumentRoutesDescriptorDto$Builder",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.DirtinessDescriptorDto$Builder",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.DirtinessDescriptorDto",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.DocumentRoutesDescriptorDto",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.DocumentRouteDescriptorDto$Builder",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.DocumentRouteDescriptorDto",
            "org.hibernate.search.mapper.orm.coordination.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto$Builder");

}

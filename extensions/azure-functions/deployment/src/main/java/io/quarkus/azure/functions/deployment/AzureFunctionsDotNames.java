package io.quarkus.azure.functions.deployment;

import org.jboss.jandex.DotName;

import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.KafkaTrigger;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.functions.annotation.WarmupTrigger;

public final class AzureFunctionsDotNames {
    public static final DotName BLOB_TRIGGER = DotName.createSimple(BlobTrigger.class.getName());
    public static final DotName COSMOS_DB_TRIGGER = DotName.createSimple(CosmosDBTrigger.class.getName());
    public static final DotName EVENT_GRID_TRIGGER = DotName.createSimple(EventGridTrigger.class.getName());
    public static final DotName EVENT_HUB_TRIGGER = DotName.createSimple(EventHubTrigger.class.getName());
    public static final DotName FUNCTION_NAME = DotName.createSimple(FunctionName.class.getName());
    public static final DotName HTTP_TRIGGER = DotName.createSimple(HttpTrigger.class.getName());
    public static final DotName KAFKA_TRIGGER = DotName.createSimple(KafkaTrigger.class.getName());
    public static final DotName QUEUE_TRIGGER = DotName.createSimple(QueueTrigger.class.getName());
    public static final DotName SERVICE_BUS_QUEUE_TRIGGER = DotName.createSimple(ServiceBusQueueTrigger.class.getName());
    public static final DotName SERVICE_BUS_TOPIC_TRIGGER = DotName.createSimple(ServiceBusQueueTrigger.class.getName());
    public static final DotName TIMER_TRIGGER = DotName.createSimple(TimerTrigger.class.getName());
    public static final DotName WARMUP_TRIGGER = DotName.createSimple(WarmupTrigger.class.getName());

    static final DotName[] PARAMETER_ANNOTATIONS = {
            BLOB_TRIGGER,
            COSMOS_DB_TRIGGER,
            EVENT_GRID_TRIGGER,
            EVENT_HUB_TRIGGER,
            HTTP_TRIGGER,
            KAFKA_TRIGGER,
            QUEUE_TRIGGER,
            SERVICE_BUS_QUEUE_TRIGGER,
            SERVICE_BUS_TOPIC_TRIGGER,
            TIMER_TRIGGER,
            WARMUP_TRIGGER
    };
}

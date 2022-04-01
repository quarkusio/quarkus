package io.quarkus.scheduler.deployment;

import java.util.concurrent.CompletionStage;

import org.jboss.jandex.DotName;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.NonBlocking;

class SchedulerDotNames {

    static final DotName SCHEDULED_NAME = DotName.createSimple(Scheduled.class.getName());
    static final DotName SCHEDULES_NAME = DotName.createSimple(Scheduled.Schedules.class.getName());
    static final DotName SKIP_NEVER_NAME = DotName.createSimple(Scheduled.Never.class.getName());
    static final DotName SKIP_PREDICATE = DotName.createSimple(Scheduled.SkipPredicate.class.getName());
    static final DotName NON_BLOCKING = DotName.createSimple(NonBlocking.class.getName());
    static final DotName UNI = DotName.createSimple("io.smallrye.mutiny.Uni");
    static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    static final DotName VOID = DotName.createSimple(Void.class.getName());

}

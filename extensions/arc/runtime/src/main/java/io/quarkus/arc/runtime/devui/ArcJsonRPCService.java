package io.quarkus.arc.runtime.devui;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.arc.runtime.devconsole.Invocation;
import io.quarkus.arc.runtime.devconsole.InvocationsMonitor;
import io.quarkus.arc.runtime.devmode.EventInfo;
import io.quarkus.arc.runtime.devmode.EventsMonitor;
import io.quarkus.arc.runtime.devmode.InvocationInfo;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;

public class ArcJsonRPCService {

    @Inject
    Instance<EventsMonitor> eventsMonitor;

    @Inject
    Instance<InvocationsMonitor> invocationsMonitor;

    public Multi<EventInfo> streamEvents() {
        return eventsMonitor.isResolvable() ? eventsMonitor.get().streamEvents() : Multi.createFrom().empty();
    }

    public Multi<Boolean> streamSkipContextEvents() {
        return eventsMonitor.isResolvable() ? eventsMonitor.get().streamSkipContextEvents() : Multi.createFrom().empty();
    }

    @NonBlocking
    public List<EventInfo> getLastEvents() {
        if (eventsMonitor.isResolvable()) {
            return eventsMonitor.get().getLastEvents();
        }
        return List.of();
    }

    @NonBlocking
    public List<EventInfo> clearLastEvents() {
        if (eventsMonitor.isResolvable()) {
            eventsMonitor.get().clear();
            return getLastEvents();
        }
        return List.of();
    }

    @NonBlocking
    public List<EventInfo> toggleSkipContextEvents() {
        if (eventsMonitor.isResolvable()) {
            eventsMonitor.get().toggleSkipContextEvents();
            return getLastEvents();
        }
        return List.of();
    }

    @NonBlocking
    public List<InvocationInfo> getLastInvocations() {
        if (invocationsMonitor.isResolvable()) {
            List<Invocation> lastInvocations = invocationsMonitor.get().getLastInvocations();
            return toInvocationInfos(lastInvocations);
        }
        return List.of();
    }

    @NonBlocking
    public List<InvocationInfo> clearLastInvocations() {
        if (invocationsMonitor.isResolvable()) {
            invocationsMonitor.get().clear();
            return getLastInvocations();
        }
        return List.of();
    }

    private List<InvocationInfo> toInvocationInfos(List<Invocation> invocations) {
        List<InvocationInfo> infos = new ArrayList<>();
        for (Invocation invocation : invocations) {
            infos.add(toInvocationInfo(invocation));
        }
        return infos;
    }

    private InvocationInfo toInvocationInfo(Invocation invocation) {
        InvocationInfo info = new InvocationInfo();
        info.setStartTime(
                timeString(LocalDateTime.ofInstant(Instant.ofEpochMilli(invocation.getStart()), ZoneId.systemDefault())));
        info.setMethodName(invocation.getDeclaringClassName() + "#" + invocation.getMethod().getName());
        info.setDuration(invocation.getDurationMillis());
        info.setKind(invocation.getKind().toString());
        info.setChildren(toInvocationInfos(invocation.getChildren()));
        info.setQuarkusBean(invocation.isQuarkusBean());
        return info;
    }

    private String timeString(LocalDateTime time) {
        String timestamp = time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ");
        int lastIndexOfDot = timestamp.lastIndexOf(".");
        return lastIndexOfDot > 0 ? timestamp.substring(0, lastIndexOfDot) : timestamp;
    }
}

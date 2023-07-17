package io.quarkus.narayana.lra.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry;

@Dependent
public class NarayanaLRAProducers {
    @Produces
    public LRAParticipantRegistry lraParticipantRegistry() {
        return NarayanaLRARecorder.registry;
    }
}

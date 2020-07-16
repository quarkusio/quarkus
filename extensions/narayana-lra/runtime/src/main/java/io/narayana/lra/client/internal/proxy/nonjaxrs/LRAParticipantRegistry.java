package io.narayana.lra.client.internal.proxy.nonjaxrs;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is tracking individual collected LRA participants that
 * contain one or more non-JAX-RS participant methods in their definitions.
 */
public class LRAParticipantRegistry {

    private final Map<String, LRAParticipant> lraParticipants;

    // required for Weld to be able to create proxy
    public LRAParticipantRegistry() {
        lraParticipants = new HashMap<>();
    }

    public LRAParticipantRegistry(Map<String, LRAParticipant> lraParticipants) {
        this.lraParticipants = new HashMap<>(lraParticipants);
    }

    public LRAParticipant getParticipant(String id) {
        return lraParticipants.get(id);
    }
}

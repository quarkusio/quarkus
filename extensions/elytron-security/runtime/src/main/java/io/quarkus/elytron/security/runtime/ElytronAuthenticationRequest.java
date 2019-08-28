package io.quarkus.elytron.security.runtime;

import org.wildfly.security.evidence.Evidence;

import io.quarkus.security.identity.request.AuthenticationRequest;

public class ElytronAuthenticationRequest implements AuthenticationRequest {

    private final String name;
    private final Evidence evidence;

    public ElytronAuthenticationRequest(String name, Evidence evidence) {
        this.name = name;
        this.evidence = evidence;
    }

    public ElytronAuthenticationRequest(Evidence evidence) {
        this.name = null;
        this.evidence = evidence;
    }

    public String getName() {
        return name;
    }

    public Evidence getEvidence() {
        return evidence;
    }
}

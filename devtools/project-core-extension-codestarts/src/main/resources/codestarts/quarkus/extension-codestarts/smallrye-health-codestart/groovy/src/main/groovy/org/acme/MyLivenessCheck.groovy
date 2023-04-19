package org.acme

import groovy.transform.CompileStatic

import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness

@CompileStatic
@Liveness
class MyLivenessCheck implements HealthCheck {

    @Override
    HealthCheckResponse call() {
        HealthCheckResponse.up("alive")
    }

}
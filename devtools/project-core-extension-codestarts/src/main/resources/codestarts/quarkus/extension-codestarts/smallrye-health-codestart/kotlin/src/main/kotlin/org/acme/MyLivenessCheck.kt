package org.acme

import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness

@Liveness
public class MyLivenessCheck : HealthCheck {

    override fun call(): HealthCheckResponse {
        return HealthCheckResponse.up("alive")
    }

}
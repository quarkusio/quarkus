/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package io.quarkus.tck.lra;

import static io.narayana.lra.LRAConstants.RECOVERY_COORDINATOR_PATH_NAME;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.logging.Logger;

import io.narayana.lra.LRAConstants;

public class NarayanaLRARecovery implements LRARecoveryService {
    private static final Logger log = Logger.getLogger(NarayanaLRARecovery.class);
    private static final long WAIT_CALLBACK_TIMEOUT = initWaitForCallbackTimeout();
    private static final String WAIT_CALLBACK_TIMEOUT_PROPERTY = "lra.tck.callback.timeout";
    private static final int DEFAULT_CALLBACK_TIMEOUT = 30000;

    private Client client = ClientBuilder.newClient();

    /*
     * Wait for the participant to return the callback. This method does not
     * guarantee callbacks to finish. The Participant status is not immediately
     * reflected to the LRA status, but only after a recovery scan which executes an
     * enlistment. The waiting time can be configurable by
     * lra.tck.callback.timeout property.
     */
    @Override
    public void waitForCallbacks(URI lraId) {
        log.trace("waitForCallbacks for: " + lraId.toASCIIString());
        try {
            await().atMost(Duration.ofMillis(WAIT_CALLBACK_TIMEOUT)).catchUncaughtExceptions()
                    .until(() -> {
                        try {
                            client.target(lraId).request().get();
                        } catch (NotFoundException notFoundException) {
                            // LRA not found means it has been finished
                            return true;
                        }
                        return false;
                    });
        } catch (ConditionTimeoutException e) {
            log.warn("waitForCallbacks timeout: " + e.getMessage());
        }
    }

    @Override
    public boolean waitForEndPhaseReplay(URI lraId) {
        log.info("waitForEndPhaseReplay for: " + lraId.toASCIIString());
        if (!recoverLRAs(lraId)) {
            // first recovery scan probably collided with periodic recovery which started
            // before the test execution so try once more
            return recoverLRAs(lraId);
        }
        return true;
    }

    /**
     * Invokes LRA coordinator recovery REST endpoint and returns whether the recovery of intended LRAs happened
     *
     * @param lraId the LRA id of the LRA that is intended to be recovered
     * @return true the intended LRA recovered, false otherwise
     */
    private boolean recoverLRAs(URI lraId) {
        // trigger a recovery scan
        Client recoveryCoordinatorClient = ClientBuilder.newClient();

        try {
            URI lraCoordinatorUri = LRAConstants.getLRACoordinatorUrl(lraId);
            URI recoveryCoordinatorUri = UriBuilder.fromUri(lraCoordinatorUri)
                    .path(RECOVERY_COORDINATOR_PATH_NAME).build();
            WebTarget recoveryTarget = recoveryCoordinatorClient.target(recoveryCoordinatorUri);

            // send the request to the recovery coordinator
            Response response = recoveryTarget.request().get();
            String json = response.readEntity(String.class);
            response.close();

            // intended LRA didn't recover
            return !json.contains(lraId.toASCIIString());
        } finally {
            recoveryCoordinatorClient.close();
        }
    }

    private static Integer initWaitForCallbackTimeout() {
        Config config = ConfigProvider.getConfig();
        if (config != null) {
            try {
                return config.getOptionalValue(WAIT_CALLBACK_TIMEOUT_PROPERTY, Integer.class).orElse(DEFAULT_CALLBACK_TIMEOUT);
            } catch (IllegalArgumentException e) {
                log.error("property " + WAIT_CALLBACK_TIMEOUT_PROPERTY + " not set correctly, using the default value: "
                        + DEFAULT_CALLBACK_TIMEOUT);
            }
        }
        return DEFAULT_CALLBACK_TIMEOUT;
    }
}

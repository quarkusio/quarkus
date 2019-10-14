/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.smallrye.health.runtime;

import java.io.ByteArrayOutputStream;

import javax.enterprise.inject.spi.CDI;

import io.quarkus.arc.Arc;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class SmallRyeReadinessHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        boolean activated = RequestScopeHelper.activeRequestScope();

        try {
            SmallRyeHealthReporter reporter = CDI.current().select(SmallRyeHealthReporter.class).get();
            SmallRyeHealth health = reporter.getReadiness();
            HttpServerResponse resp = event.response();
            if (health.isDown()) {
                resp.setStatusCode(503);
            }
            resp.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            reporter.reportHealth(outputStream, health);
            resp.end(Buffer.buffer(outputStream.toByteArray()));
        } finally {
            if (activated) {
                Arc.container().requestContext().terminate();
            }
        }
    }
}

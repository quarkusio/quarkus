/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.shamrock.jaeger.runtime;

import org.jboss.shamrock.runtime.annotations.Template;
import java.util.Optional;

import io.opentracing.util.GlobalTracer;

import static io.jaegertracing.Configuration.JAEGER_SERVICE_NAME;
import static io.jaegertracing.Configuration.JAEGER_ENDPOINT;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.Template;

@Template
public class JaegerDeploymentTemplate {
    private static volatile boolean registered;

    private static final Logger log = Logger.getLogger(JaegerDeploymentTemplate.class);

    public void registerTracer() {
        if (!registered) {
            if (isValidConfig()) {
                GlobalTracer.register(new ShamrockJaegerTracer());
            }
            registered = true;
        }
    }

    private static boolean isValidConfig() {
        Config config = ConfigProvider.getConfig();
        Optional<String> serviceName = config.getOptionalValue(JAEGER_SERVICE_NAME, String.class);
        Optional<String> endpoint = config.getOptionalValue(JAEGER_ENDPOINT, String.class);
        if (!serviceName.isPresent()) {
            log.warn("Property 'JAEGER_SERVICE_NAME' has not been defined");
        } else if (!endpoint.isPresent()) {
            log.warn("Property 'JAEGER_ENDPOINT' has not been defined");
            // Return true for now, so we can reproduce issue with UdpSender
            return true;
        } else {
            return true;
        }
        return false;
    }
}


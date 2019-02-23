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

package io.quarkus.jaeger.runtime;

import static io.jaegertracing.Configuration.JAEGER_ENDPOINT;
import static io.jaegertracing.Configuration.JAEGER_SERVICE_NAME;

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.opentracing.util.GlobalTracer;
import io.quarkus.runtime.annotations.Template;

@Template
public class JaegerDeploymentTemplate {
    private static volatile boolean registered;

    private static final Logger log = Logger.getLogger(JaegerDeploymentTemplate.class);

    public void registerTracer(JaegerConfig jaeger) {
        if (!registered) {
            if (isValidConfig(jaeger)) {
                initTracerConfig(jaeger);
                GlobalTracer.register(new QuarkusJaegerTracer());
            }
            registered = true;
        }
    }

    private boolean isValidConfig(JaegerConfig jaeger) {
        Config mpconfig = ConfigProvider.getConfig();
        Optional<String> serviceName = mpconfig.getOptionalValue(JAEGER_SERVICE_NAME, String.class);
        Optional<String> endpoint = mpconfig.getOptionalValue(JAEGER_ENDPOINT, String.class);
        if (!jaeger.serviceName.isPresent() && !serviceName.isPresent()) {
            log.warn(
                    "Jaeger service name has not been defined (e.g. JAEGER_SERVICE_NAME environment variable or system properties)");
        } else if (!jaeger.endpoint.isPresent() && !endpoint.isPresent()) {
            log.warn(
                    "Jaeger collector endpoint has not been defined (e.g. JAEGER_ENDPOINT environment variable or system properties)");
            // Return true for now, so we can reproduce issue with UdpSender
            return true;
        } else {
            return true;
        }
        return false;
    }

    private void initTracerConfig(JaegerConfig jaeger) {
        initTracerProperty("JAEGER_ENDPOINT", jaeger.endpoint, uri -> uri.toString());
        initTracerProperty("JAEGER_AUTH_TOKEN", jaeger.authToken, token -> token);
        initTracerProperty("JAEGER_USER", jaeger.user, user -> user);
        initTracerProperty("JAEGER_PASSWORD", jaeger.password, pw -> pw);
        initTracerProperty("JAEGER_AGENT_HOST", jaeger.agentHostPort, address -> address.getHostName());
        initTracerProperty("JAEGER_AGENT_PORT", jaeger.agentHostPort, address -> String.valueOf(address.getPort()));
        initTracerProperty("JAEGER_REPORTER_LOG_SPANS", jaeger.reporterLogSpans, log -> log.toString());
        initTracerProperty("JAEGER_REPORTER_MAX_QUEUE_SIZE", jaeger.reporterMaxQueueSize, size -> size.toString());
        initTracerProperty("JAEGER_REPORTER_FLUSH_INTERVAL", jaeger.reporterFlushInterval,
                duration -> String.valueOf(duration.toMillis()));
        initTracerProperty("JAEGER_SAMPLER_TYPE", jaeger.samplerType, type -> type);
        initTracerProperty("JAEGER_SAMPLER_PARAM", jaeger.samplerParam, param -> param.toString());
        initTracerProperty("JAEGER_SAMPLER_MANAGER_HOST_PORT", jaeger.samplerManagerHostPort, hostPort -> hostPort.toString());
        initTracerProperty("JAEGER_SERVICE_NAME", jaeger.serviceName, name -> name);
        initTracerProperty("JAEGER_TAGS", jaeger.tags, tags -> tags.toString());
        initTracerProperty("JAEGER_PROPAGATION", jaeger.propagation, format -> format.toString());
        initTracerProperty("JAEGER_SENDER_FACTORY", jaeger.senderFactory, sender -> sender);
    }

    private <T> void initTracerProperty(String property, Optional<T> value, Function<T, String> accessor) {
        if (value.isPresent()) {
            System.setProperty(property, accessor.apply(value.get()));
        }
    }
}

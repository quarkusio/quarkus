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

import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;

import javax.inject.Inject;

import io.jaegertracing.Configuration;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ShamrockJaegerTracer implements Tracer {

    static AtomicReference<Tracer> REF = new AtomicReference<>();

    static String[] CONFIG_PROPERTIES = {
        Configuration.JAEGER_ENDPOINT,
        Configuration.JAEGER_AUTH_TOKEN,
        Configuration.JAEGER_USER,
        Configuration.JAEGER_PASSWORD,
        Configuration.JAEGER_AGENT_HOST,
        Configuration.JAEGER_AGENT_PORT,
        Configuration.JAEGER_REPORTER_LOG_SPANS,
        Configuration.JAEGER_REPORTER_MAX_QUEUE_SIZE,
        Configuration.JAEGER_REPORTER_FLUSH_INTERVAL,
        Configuration.JAEGER_SAMPLER_TYPE,
        Configuration.JAEGER_SAMPLER_PARAM,
        Configuration.JAEGER_SAMPLER_MANAGER_HOST_PORT,
        Configuration.JAEGER_SERVICE_NAME,
        Configuration.JAEGER_TAGS,
        Configuration.JAEGER_PROPAGATION,
        Configuration.JAEGER_SENDER_FACTORY
    };

    public ShamrockJaegerTracer() {
    }

    @Override
    public String toString() {
        return "Jaeger Tracer";
    }

    Tracer tracer() {
        return REF.updateAndGet((orig) -> {
            if (orig != null) {
                return orig;
            }

            Config config = ConfigProvider.getConfig();
            // Copy the Jaeger properties from MP-Config into system properties for
            // use by the Jaeger configuration builder.
            for (int i=0; i < CONFIG_PROPERTIES.length; i++) {
                Optional<String> val = config.getOptionalValue(CONFIG_PROPERTIES[i], String.class);
                if (val.isPresent()) {
                    System.setProperty(CONFIG_PROPERTIES[i], val.get());
                }
            }

            return Configuration.fromEnv().withMetricsFactory(new ShamrockJaegerMetricsFactory()).getTracer();
        });
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return tracer().buildSpan(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        tracer().inject(spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        return tracer().extract(format, carrier);
    }

    @Override
    public ScopeManager scopeManager() {
        return tracer().scopeManager();
    }

    @Override
    public Span activeSpan() {
        return tracer().activeSpan();
    }
}


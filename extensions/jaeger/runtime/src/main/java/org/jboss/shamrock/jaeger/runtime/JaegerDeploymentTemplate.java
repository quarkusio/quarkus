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

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.Template;
import java.util.Optional;

import io.opentracing.util.GlobalTracer;

import static io.jaegertracing.Configuration.JAEGER_SERVICE_NAME;
import static io.jaegertracing.Configuration.JAEGER_ENDPOINT;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import org.jboss.logging.Logger;

@Template
public class JaegerDeploymentTemplate {
    private static volatile boolean registered;

    private static final Logger log = Logger.getLogger(JaegerDeploymentTemplate.class);

    public void registerTracer(JaegerConfig jaegerConfig) {
        if (!registered) {
            if (isValidConfig(jaegerConfig)) {
                GlobalTracer.register(new ShamrockJaegerTracer());
            }
            registered = true;
        }
    }

    private boolean isValidConfig(JaegerConfig config) {
        System.out.println("JaegerDeploymentTemplate config = "+config);
        System.out.println("JaegerDeploymentTemplate config.serviceName = " + config.serviceName);
        System.out.println("JaegerDeploymentTemplate config.jaegerServiceName = " + config.jaegerServiceName);
        Config mpconfig = ConfigProvider.getConfig();
        Optional<String> serviceName = mpconfig.getOptionalValue(JAEGER_SERVICE_NAME, String.class);
        System.out.println("MP-config JAEGER_SERVICE_NAME = " + serviceName);
        Optional<String> endpoint = mpconfig.getOptionalValue(JAEGER_ENDPOINT, String.class);
        if (!config.serviceName.isPresent() && !serviceName.isPresent()) {
            log.warn("Jaeger service name has not been defined (e.g. JAEGER_SERVICE_NAME environment variable or system properties)");
        } else if (!config.endpoint.isPresent() && !endpoint.isPresent()) {
            log.warn("Jaeger collector endpoint has not been defined (e.g. JAEGER_ENDPOINT environment variable or system properties)");
            // Return true for now, so we can reproduce issue with UdpSender
            return true;
        } else {
            return true;
        }
        return false;
    }
}


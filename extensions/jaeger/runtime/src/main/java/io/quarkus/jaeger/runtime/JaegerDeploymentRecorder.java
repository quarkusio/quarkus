package io.quarkus.jaeger.runtime;

import java.util.Optional;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.opentracing.util.GlobalTracer;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaegerDeploymentRecorder {
    private static final Logger log = Logger.getLogger(JaegerDeploymentRecorder.class);
    private static final Optional UNKNOWN_SERVICE_NAME = Optional.of("quarkus/unknown");
    private static final QuarkusJaegerTracer quarkusTracer = new QuarkusJaegerTracer();

    synchronized public void registerTracer(JaegerConfig jaeger, ApplicationConfig appConfig) {
        if (!jaeger.serviceName.isPresent()) {
            if (appConfig.name.isPresent()) {
                jaeger.serviceName = appConfig.name;
            } else {
                jaeger.serviceName = UNKNOWN_SERVICE_NAME;
            }
        }
        initTracerConfig(jaeger);
        quarkusTracer.reset();
        // register Quarkus tracer to GlobalTracer.
        // Usually the tracer will be registered only here, although consumers
        // could register a different tracer in the code which runs before this class.
        // This is also used in tests.
        if (!GlobalTracer.isRegistered()) {
            log.debugf("Registering tracer to GlobalTracer %s", quarkusTracer);
            GlobalTracer.register(quarkusTracer);
        }
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
        quarkusTracer.setLogTraceContext(jaeger.logTraceContext);
    }

    private <T> void initTracerProperty(String property, Optional<T> value, Function<T, String> accessor) {
        if (value.isPresent()) {
            System.setProperty(property, accessor.apply(value.get()));
        }
    }
}

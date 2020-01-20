package io.quarkus.jaeger.runtime;

import static io.jaegertracing.Configuration.JAEGER_SERVICE_NAME;

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.opentracing.util.GlobalTracer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaegerDeploymentRecorder {
    private static volatile boolean registered;

    private static final Logger log = Logger.getLogger(JaegerDeploymentRecorder.class);

    public void registerTracer(JaegerConfig jaeger) {
        if (!registered) {
            if (isValidConfig(jaeger)) {
                initTracerConfig(jaeger);
                QuarkusJaegerTracer quarkusJaegerTracer = new QuarkusJaegerTracer();
                log.debugf("Registering tracer to GlobalTracer %s", quarkusJaegerTracer);
                GlobalTracer.register(quarkusJaegerTracer);
            }
            registered = true;
        }
    }

    private boolean isValidConfig(JaegerConfig jaeger) {
        Config mpconfig = ConfigProvider.getConfig();
        Optional<String> serviceName = mpconfig.getOptionalValue(JAEGER_SERVICE_NAME, String.class);
        if (!jaeger.serviceName.isPresent() && !serviceName.isPresent()) {
            log.warn(
                    "Jaeger service name has not been defined, either as 'quarkus.jaeger.service-name' application property or JAEGER_SERVICE_NAME environment variable/system property");
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
        initTracerProperty(QuarkusJaegerTracer.LOG_TRACE_CONTEXT, Optional.of(jaeger.logTraceContext),
                logTraceContext -> logTraceContext.toString());
    }

    private <T> void initTracerProperty(String property, Optional<T> value, Function<T, String> accessor) {
        if (value.isPresent()) {
            System.setProperty(property, accessor.apply(value.get()));
        }
    }
}

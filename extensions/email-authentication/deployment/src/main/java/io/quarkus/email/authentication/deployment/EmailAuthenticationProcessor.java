package io.quarkus.email.authentication.deployment;

import static io.quarkus.arc.processor.DotNames.SINGLETON;
import static io.quarkus.email.authentication.runtime.internal.EmailAuthenticationRecorder.LIVE_RELOAD_ENCRYPTION_KEY;

import java.io.IOException;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.email.authentication.EmailAuthenticationRequest;
import io.quarkus.email.authentication.runtime.internal.EmailAuthenticationRecorder;
import io.quarkus.mailer.MailerName;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.mailer.runtime.Mailers;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.vertx.http.deployment.VertxWebRouterBuildItem;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;

@BuildSteps(onlyIf = EmailAuthenticationProcessor.IsEmailAuthenticationEnabled.class)
class EmailAuthenticationProcessor {

    private static final String MECHANISM_NAME = "io.quarkus.email.authentication.runtime.internal.EmailAuthenticationMechanism";
    private static final String DEFAULT_STORAGE_NAME = "io.quarkus.email.authentication.runtime.internal.CookieEmailAuthenticationCodeStorage";
    private static final String DEFAULT_SENDER_NAME = "io.quarkus.email.authentication.runtime.internal.DefaultEmailAuthenticationCodeSender";
    private static final String DEFAULT_IDENTITY_PROVIDER = "io.quarkus.email.authentication.runtime.internal.EmailAuthenticationIdentityProvider";
    private static final DotName EMAIL_AUTHENTICATION_REQUEST = DotName.createSimple(EmailAuthenticationRequest.class);

    @BuildStep
    AdditionalBeanBuildItem registerCdiBeans(CombinedIndexBuildItem combinedIndexBuildItem) {
        var builder = AdditionalBeanBuildItem.builder()
                .addBeanClasses(DEFAULT_SENDER_NAME, DEFAULT_STORAGE_NAME, MECHANISM_NAME)
                .setDefaultScope(SINGLETON);
        if (foundNoCustomIdentityProvider(combinedIndexBuildItem.getIndex())) {
            builder.addBeanClass(DEFAULT_IDENTITY_PROVIDER);
        }
        return builder.build();
    }

    @Produce(ServiceStartBuildItem.class)
    @BuildStep(onlyIf = IsEagerAuthenticationDisabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerLazyAuthRouteHandler(EmailAuthenticationRecorder recorder, VertxWebRouterBuildItem vertxWebRouter) {
        recorder.registerEmailAuthRouteHandler(vertxWebRouter.getHttpRouter());
    }

    @BuildStep(onlyIf = UseNamedMailer.class)
    InjectionPointTransformerBuildItem supportNamedMailers(EmailAuthenticationBuildTimeConfig buildTimeConfig) {
        return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {

            private final DotName reactiveMailerName = DotName.createSimple(ReactiveMailer.class);
            private final String mailerName = buildTimeConfig.mailerName();

            @Override
            public boolean appliesTo(Type t) {
                return t.kind() == Type.Kind.CLASS && reactiveMailerName.equals(t.name());
            }

            @Override
            public void transform(TransformationContext ctx) {
                var annotationTarget = ctx.getAnnotationTarget();
                if (annotationTarget != null && annotationTarget.kind() == AnnotationTarget.Kind.METHOD
                        && annotationTarget.asMethod().isConstructor()) {
                    var methodInfo = annotationTarget.asMethod();
                    if (MECHANISM_NAME.equals(methodInfo.declaringClass().name().toString())) {
                        ctx.transform().add(MailerName.class, AnnotationValue.createStringValue("value", mailerName)).done();
                    }
                }
            }
        });
    }

    @BuildStep(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
    DevServicesResultBuildItem useSameEncryptionKeyAfterRestart() {
        // Quarkus should recognize that the service config didn't change (as it is a constant),
        // thus generate our encryption key only once and keep it among live reloads
        return DevServicesResultBuildItem.owned()
                .feature(Feature.SECURITY)
                .serviceConfig(1)
                .configProvider(Map.of(
                        LIVE_RELOAD_ENCRYPTION_KEY, s -> EmailAuthenticationRecorder.generateEncryptionKey()))
                .postStartHook(s -> {
                    // keep this dev service silent
                })
                .startable(() -> new Startable() {
                    @Override
                    public void start() {

                    }

                    @Override
                    public String getConnectionInfo() {
                        return "";
                    }

                    @Override
                    public String getContainerId() {
                        return "";
                    }

                    @Override
                    public void close() throws IOException {

                    }
                })
                .build();
    }

    private static boolean foundNoCustomIdentityProvider(IndexView index) {
        return index.getAllKnownImplementations(IdentityProvider.class).stream().noneMatch(p -> {
            if (!p.isInterface() && !p.isAbstract() && !p.interfaceTypes().isEmpty()) {
                for (Type interfaceType : p.interfaceTypes()) {
                    if (interfaceType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        var parametrizedType = interfaceType.asParameterizedType();
                        for (Type argument : parametrizedType.arguments()) {
                            if (EMAIL_AUTHENTICATION_REQUEST.equals(argument.name())) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        });
    }

    static final class IsEmailAuthenticationEnabled implements BooleanSupplier {

        private final boolean enabled;

        IsEmailAuthenticationEnabled(EmailAuthenticationBuildTimeConfig buildTimeConfig) {
            this.enabled = buildTimeConfig.enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }

    static final class UseNamedMailer implements BooleanSupplier {

        private final boolean useNamedMailer;

        UseNamedMailer(EmailAuthenticationBuildTimeConfig buildTimeConfig) {
            this.useNamedMailer = !Mailers.DEFAULT_MAILER_NAME.equals(buildTimeConfig.mailerName());
        }

        @Override
        public boolean getAsBoolean() {
            return useNamedMailer;
        }
    }

    static final class IsEagerAuthenticationDisabled implements BooleanSupplier {

        private final boolean disabled;

        IsEagerAuthenticationDisabled(VertxHttpBuildTimeConfig buildTimeConfig) {
            this.disabled = !buildTimeConfig.auth().proactive();
        }

        @Override
        public boolean getAsBoolean() {
            return disabled;
        }
    }
}

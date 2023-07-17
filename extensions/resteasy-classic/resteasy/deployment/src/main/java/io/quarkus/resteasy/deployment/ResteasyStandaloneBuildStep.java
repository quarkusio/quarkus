package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Optional;

import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.runtime.AuthenticationCompletionExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationFailedExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationRedirectExceptionMapper;
import io.quarkus.resteasy.runtime.ResteasyVertxConfig;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ResteasyStandaloneBuildStep {

    private static final int REST_ROUTE_ORDER_OFFSET = 500;
    private static final DotName EXCEPTION_MAPPER = DotName.createSimple(ExceptionMapper.class.getName());

    public static final class ResteasyStandaloneBuildItem extends SimpleBuildItem {

        final String deploymentRootPath;

        public ResteasyStandaloneBuildItem(String deploymentRootPath) {
            this.deploymentRootPath = deploymentRootPath.startsWith("/") ? deploymentRootPath : "/" + deploymentRootPath;
        }

    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void staticInit(ResteasyStandaloneRecorder recorder,
            Capabilities capabilities,
            ResteasyDeploymentBuildItem deployment,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady,
            HttpRootPathBuildItem httpRootPathBuildItem,
            BuildProducer<ResteasyStandaloneBuildItem> standalone) throws Exception {
        if (capabilities.isPresent(Capability.SERVLET)) {
            return;
        }

        if (deployment != null) {
            // the deployment path is always relative to the HTTP root path
            recorder.staticInit(deployment.getDeployment(),
                    httpRootPathBuildItem.relativePath(deployment.getRootPath()));
            standalone.produce(new ResteasyStandaloneBuildItem(deployment.getRootPath()));
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            ResteasyStandaloneRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer,
            CoreVertxBuildItem vertx,
            CombinedIndexBuildItem combinedIndexBuildItem,
            HttpBuildTimeConfig vertxConfig,
            ResteasyStandaloneBuildItem standalone,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            ExecutorBuildItem executorBuildItem,
            ResteasyVertxConfig resteasyVertxConfig,
            HttpBuildTimeConfig httpBuildTimeConfig) throws Exception {

        if (standalone == null) {
            return;
        }
        feature.produce(new FeatureBuildItem(Feature.RESTEASY));

        // Handler used for both the default and non-default deployment path (specified as application path or resteasyConfig.path)
        // Routes use the order VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1 to ensure the default route is called before the resteasy one
        Handler<RoutingContext> handler = recorder.vertxRequestHandler(vertx.getVertx(),
                executorBuildItem.getExecutorProxy(), resteasyVertxConfig, httpBuildTimeConfig);

        final boolean noCustomAuthCompletionExMapper;
        final boolean noCustomAuthFailureExMapper;
        final boolean noCustomAuthRedirectExMapper;
        if (vertxConfig.auth.proactive) {
            noCustomAuthCompletionExMapper = notFoundCustomExMapper(AuthenticationCompletionException.class.getName(),
                    AuthenticationCompletionExceptionMapper.class.getName(), combinedIndexBuildItem.getIndex());
            noCustomAuthFailureExMapper = notFoundCustomExMapper(AuthenticationFailedException.class.getName(),
                    AuthenticationFailedExceptionMapper.class.getName(), combinedIndexBuildItem.getIndex());
            noCustomAuthRedirectExMapper = notFoundCustomExMapper(AuthenticationRedirectException.class.getName(),
                    AuthenticationRedirectExceptionMapper.class.getName(), combinedIndexBuildItem.getIndex());
        } else {
            // with disabled proactive auth we need to handle exceptions anyway as default auth failure handler did not
            noCustomAuthCompletionExMapper = false;
            noCustomAuthFailureExMapper = false;
            noCustomAuthRedirectExMapper = false;
        }
        // failure handler for auth failures that occurred before the handler defined right above started processing the request
        // we add the failure handler right before QuarkusErrorHandler
        // so that user can define failure handlers that precede exception mappers
        final Handler<RoutingContext> failureHandler = recorder.vertxFailureHandler(vertx.getVertx(),
                executorBuildItem.getExecutorProxy(), resteasyVertxConfig, noCustomAuthCompletionExMapper,
                noCustomAuthFailureExMapper, noCustomAuthRedirectExMapper, vertxConfig.auth.proactive);
        filterBuildItemBuildProducer.produce(FilterBuildItem.ofAuthenticationFailureHandler(failureHandler));

        // Exact match for resources matched to the root path
        routes.produce(
                RouteBuildItem.builder()
                        .orderedRoute(standalone.deploymentRootPath,
                                VertxHttpRecorder.AFTER_DEFAULT_ROUTE_ORDER_MARK + REST_ROUTE_ORDER_OFFSET)
                        .handler(handler).build());
        String matchPath = standalone.deploymentRootPath;
        if (matchPath.endsWith("/")) {
            matchPath += "*";
        } else {
            matchPath += "/*";
        }
        // Match paths that begin with the deployment path
        routes.produce(RouteBuildItem.builder()
                .orderedRoute(matchPath, VertxHttpRecorder.AFTER_DEFAULT_ROUTE_ORDER_MARK + REST_ROUTE_ORDER_OFFSET)
                .handler(handler).build());

        recorder.start(shutdown, requireVirtual.isPresent());
    }

    private static boolean notFoundCustomExMapper(String exSignatureStr, String exMapperSignatureStr, IndexView index) {
        for (var implementor : index.getAllKnownImplementors(EXCEPTION_MAPPER)) {
            if (exMapperSignatureStr.equals(implementor.name().toString())) {
                continue;
            }
            for (Type interfaceType : implementor.interfaceTypes()) {
                if (EXCEPTION_MAPPER.equals(interfaceType.name())) {
                    final String mapperExSignature = interfaceType.asParameterizedType().arguments().get(0).name().toString();
                    if (exSignatureStr.equals(mapperExSignature)) {
                        return false;
                    }
                    break;
                }
            }
        }
        return true;
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    public FilterBuildItem addDefaultAuthFailureHandler(ResteasyStandaloneRecorder recorder) {
        // replace default auth failure handler added by vertx-http so that our exception mappers can customize response
        return new FilterBuildItem(recorder.defaultAuthFailureHandler(), FilterBuildItem.AUTHENTICATION - 1);
    }
}

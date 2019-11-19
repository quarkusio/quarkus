package io.quarkus.amazon.lambda.http.deployment;

import org.jboss.logging.Logger;

import io.quarkus.amazon.lambda.deployment.ProvidedAmazonLambdaHandlerBuildItem;
import io.quarkus.amazon.lambda.http.LambdaHttpHandler;
import io.quarkus.amazon.lambda.http.model.AlbContext;
import io.quarkus.amazon.lambda.http.model.ApiGatewayAuthorizerContext;
import io.quarkus.amazon.lambda.http.model.ApiGatewayRequestIdentity;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.CognitoAuthorizerClaims;
import io.quarkus.amazon.lambda.http.model.ErrorModel;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.http.model.MultiValuedTreeMap;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;
import io.vertx.core.file.impl.FileResolver;

public class AmazonLambdaHttpProcessor {
    private static final Logger log = Logger.getLogger(AmazonLambdaHttpProcessor.class);

    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode() == LaunchMode.NORMAL ? RequireVirtualHttpBuildItem.MARKER : null;
    }

    @BuildStep
    public ProvidedAmazonLambdaHandlerBuildItem setHandler() {
        return new ProvidedAmazonLambdaHandlerBuildItem(LambdaHttpHandler.class, "AWS Lambda HTTP");
    }

    @BuildStep
    public void registerReflectionClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
        reflectiveClassBuildItemBuildProducer
                .produce(new ReflectiveClassBuildItem(true, true, true,
                        AlbContext.class,
                        ApiGatewayAuthorizerContext.class,
                        ApiGatewayRequestIdentity.class,
                        AwsProxyRequest.class,
                        AwsProxyRequestContext.class,
                        AwsProxyResponse.class,
                        CognitoAuthorizerClaims.class,
                        ErrorModel.class,
                        Headers.class,
                        MultiValuedTreeMap.class));
    }

    /**
     * Lambda provides /tmp for temporary files. Set vertx cache dir
     */
    @BuildStep
    void setTempDir(BuildProducer<SystemPropertyBuildItem> systemProperty) {
        systemProperty.produce(new SystemPropertyBuildItem(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp"));
    }

}

package io.quarkus.amazon.lambda.http.deployment;

import org.jboss.logging.Logger;

import io.quarkus.amazon.lambda.deployment.LambdaUtil;
import io.quarkus.amazon.lambda.deployment.ProvidedAmazonLambdaHandlerBuildItem;
import io.quarkus.amazon.lambda.http.DefaultLambdaIdentityProvider;
import io.quarkus.amazon.lambda.http.LambdaHttpAuthenticationMechanism;
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
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;
import io.vertx.core.file.impl.FileResolver;

public class AmazonLambdaHttpProcessor {
    private static final Logger log = Logger.getLogger(AmazonLambdaHttpProcessor.class);

    @BuildStep
    public void setupSecurity(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            LambdaHttpBuildTimeConfig config) {
        if (!config.enableSecurity)
            return;

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(LambdaHttpAuthenticationMechanism.class)
                .addBeanClass(DefaultLambdaIdentityProvider.class);
        additionalBeans.produce(builder.build());
    }

    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp() {
        return RequireVirtualHttpBuildItem.ALWAYS_VIRTUAL;
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
        systemProperty.produce(new SystemPropertyBuildItem(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/quarkus"));
    }

    @BuildStep
    public void generateScripts(OutputTargetBuildItem target,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) throws Exception {
        String lambdaName = LambdaUtil.artifactToLambda(target.getBaseName());

        String output = LambdaUtil.copyResource("lambda/bootstrap-example.sh");
        LambdaUtil.writeFile(target, "bootstrap-example.sh", output);

        output = LambdaUtil.copyResource("http/sam.jvm.yaml")
                .replace("${lambdaName}", lambdaName);
        LambdaUtil.writeFile(target, "sam.jvm.yaml", output);

        output = LambdaUtil.copyResource("http/sam.native.yaml")
                .replace("${lambdaName}", lambdaName);
        LambdaUtil.writeFile(target, "sam.native.yaml", output);
    }
}

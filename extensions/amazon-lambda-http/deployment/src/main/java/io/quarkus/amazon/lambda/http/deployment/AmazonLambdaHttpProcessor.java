package io.quarkus.amazon.lambda.http.deployment;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.quarkus.amazon.lambda.deployment.LambdaUtil;
import io.quarkus.amazon.lambda.deployment.ProvidedAmazonLambdaHandlerBuildItem;
import io.quarkus.amazon.lambda.http.LambdaHttpHandler;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.http.model.MultiValuedTreeMap;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
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
                        APIGatewayV2HTTPEvent.class,
                        APIGatewayV2HTTPEvent.RequestContext.class,
                        APIGatewayV2HTTPEvent.RequestContext.Http.class,
                        APIGatewayV2HTTPEvent.RequestContext.Authorizer.class,
                        APIGatewayV2HTTPEvent.RequestContext.CognitoIdentity.class,
                        APIGatewayV2HTTPEvent.RequestContext.IAM.class,
                        APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT.class,
                        APIGatewayV2HTTPResponse.class, Headers.class, MultiValuedTreeMap.class));
    }

    /**
     * Lambda provides /tmp for temporary files. Set vertx cache dir
     */
    @BuildStep
    void setTempDir(BuildProducer<SystemPropertyBuildItem> systemProperty) {
        systemProperty.produce(new SystemPropertyBuildItem(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp"));
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

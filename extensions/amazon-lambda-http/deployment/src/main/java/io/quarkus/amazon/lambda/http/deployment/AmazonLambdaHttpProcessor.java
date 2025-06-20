package io.quarkus.amazon.lambda.http.deployment;

import static io.vertx.core.file.impl.FileResolverImpl.CACHE_DIR_BASE_PROP_NAME;

import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.quarkus.amazon.lambda.deployment.LambdaUtil;
import io.quarkus.amazon.lambda.deployment.ProvidedAmazonLambdaHandlerBuildItem;
import io.quarkus.amazon.lambda.http.AwsHttpContextProducers;
import io.quarkus.amazon.lambda.http.DefaultLambdaIdentityProvider;
import io.quarkus.amazon.lambda.http.LambdaHttpAuthenticationMechanism;
import io.quarkus.amazon.lambda.http.LambdaHttpHandler;
import io.quarkus.amazon.lambda.http.LambdaHttpRecorder;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.http.model.MultiValuedTreeMap;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.resteasy.reactive.server.spi.ContextTypeBuildItem;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class AmazonLambdaHttpProcessor {
    private static final Logger log = Logger.getLogger(AmazonLambdaHttpProcessor.class);
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName API_GATEWAY_HTTP_EVENT = DotName.createSimple(APIGatewayV2HTTPEvent.class.getName());
    public static final DotName REQUEST_CONTEXT = DotName.createSimple(
            APIGatewayV2HTTPEvent.RequestContext.class.getName());

    @BuildStep
    public void setupCDI(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClasses(AwsHttpContextProducers.class);
        additionalBeans.produce(builder.build());
    }

    @BuildStep
    public void setupSecurity(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            LambdaHttpBuildTimeConfig config) {
        if (!config.enableSecurity())
            return;

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        builder.addBeanClasses(LambdaHttpAuthenticationMechanism.class,
                DefaultLambdaIdentityProvider.class,
                AwsHttpContextProducers.class);
        additionalBeans.produce(builder.build());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void setupConfig(LambdaHttpRecorder recorder) {
        // force config to be set as static var in the recorder - TODO - rewrite this, it shouldn't use static vars
        recorder.setConfig();
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
                .produce(ReflectiveClassBuildItem.builder(APIGatewayV2HTTPEvent.class,
                        APIGatewayV2HTTPEvent.RequestContext.class,
                        APIGatewayV2HTTPEvent.RequestContext.Http.class,
                        APIGatewayV2HTTPEvent.RequestContext.Authorizer.class,
                        APIGatewayV2HTTPEvent.RequestContext.CognitoIdentity.class,
                        APIGatewayV2HTTPEvent.RequestContext.IAM.class,
                        APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT.class,
                        APIGatewayV2HTTPResponse.class, Headers.class, MultiValuedTreeMap.class)
                        .reason(getClass().getName())
                        .methods().fields().build());
    }

    /**
     * Lambda provides /tmp for temporary files. Set vertx cache dir
     */
    @BuildStep
    void setTempDir(BuildProducer<SystemPropertyBuildItem> systemProperty) {
        systemProperty.produce(new SystemPropertyBuildItem(CACHE_DIR_BASE_PROP_NAME, "/tmp/quarkus"));
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

    @BuildStep
    public void resteasyReactiveIntegration(BuildProducer<ContextTypeBuildItem> contextTypeProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanProducer) {
        contextTypeProducer.produce(new ContextTypeBuildItem(CONTEXT));
        contextTypeProducer.produce(new ContextTypeBuildItem(API_GATEWAY_HTTP_EVENT));
        contextTypeProducer.produce(new ContextTypeBuildItem(REQUEST_CONTEXT));

        unremovableBeanProducer.produce(UnremovableBeanBuildItem.beanTypes(CONTEXT, API_GATEWAY_HTTP_EVENT, REQUEST_CONTEXT));
    }

}

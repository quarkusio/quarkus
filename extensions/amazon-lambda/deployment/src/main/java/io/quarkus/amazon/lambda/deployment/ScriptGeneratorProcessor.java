package io.quarkus.amazon.lambda.deployment;

import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class ScriptGeneratorProcessor {
    @BuildStep
    public void buildScripts(OutputTargetBuildItem target,
            Optional<ProvidedAmazonLambdaHandlerBuildItem> providedLambda,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) throws Exception {
        if (providedLambda.isPresent())
            return; // assume these will be generated elsewhere
        LambdaUtil.generateScripts("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest", target);
    }

}

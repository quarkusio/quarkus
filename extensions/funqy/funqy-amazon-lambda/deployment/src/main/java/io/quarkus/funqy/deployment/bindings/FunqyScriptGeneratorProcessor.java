package io.quarkus.funqy.deployment.bindings;

import io.quarkus.amazon.lambda.deployment.LambdaUtil;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class FunqyScriptGeneratorProcessor {
    @BuildStep
    public void buildScripts(OutputTargetBuildItem target,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) throws Exception {
        LambdaUtil.generateScripts("io.quarkus.funqy.lambda.FunqyStreamHandler::handleRequest", target);
    }
}

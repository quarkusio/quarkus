package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

public class ConditionalDependenciesDirectDependencyOnTransitiveDeploymentArtifactTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsQuarkusExt quarkusCore = new TsQuarkusExt("quarkus-core");
        install(quarkusCore);

        TsArtifact nettyNioClient = TsArtifact.jar("netty-nio-client");

        final TsQuarkusExt nettyClientInternalExt = new TsQuarkusExt("netty-client-internal");
        nettyClientInternalExt.addDependency(quarkusCore);
        nettyClientInternalExt.getRuntime().addDependency(nettyNioClient, true);
        nettyClientInternalExt.setDependencyCondition(nettyNioClient);
        install(nettyClientInternalExt, false);
        addCollectedDep(nettyClientInternalExt.getRuntime(),
                DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(nettyClientInternalExt.getDeployment());

        final TsQuarkusExt commonExt = new TsQuarkusExt("common");
        commonExt.addDependency(quarkusCore);
        commonExt.getRuntime().addDependency(nettyNioClient, true);
        commonExt.getRuntime().addDependency(nettyClientInternalExt.getRuntime(), true);
        commonExt.getDeployment().addDependency(nettyClientInternalExt.getDeployment(), true);
        commonExt.setConditionalDeps(nettyClientInternalExt);
        install(commonExt, false);
        addCollectedDep(commonExt.getRuntime(),
                DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(commonExt.getDeployment());

        final TsQuarkusExt sqsExt = new TsQuarkusExt("sqs");
        sqsExt.addDependency(quarkusCore);
        sqsExt.getRuntime().addDependency(commonExt.getRuntime());
        sqsExt.getRuntime().addDependency(nettyNioClient, true);
        sqsExt.getDeployment().addFirstDependency(commonExt.getDeployment());
        addCollectedDep(sqsExt.getRuntime(),
                DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(sqsExt.getDeployment());

        final TsQuarkusExt messagingSqsExt = new TsQuarkusExt("messaging-sqs");
        messagingSqsExt.addDependency(quarkusCore);
        messagingSqsExt.addDependency(sqsExt);
        messagingSqsExt.getDeployment().addDependency(commonExt.getDeployment()); // this line breaks it

        installAsDep(messagingSqsExt);
        installAsDep(nettyNioClient);
    }
}

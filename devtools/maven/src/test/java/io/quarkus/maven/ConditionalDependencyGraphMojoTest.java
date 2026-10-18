package io.quarkus.maven;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

public class ConditionalDependencyGraphMojoTest extends DependencyTreeMojoTestBase {
    @Override
    protected String mode() {
        return "prod";
    }

    @Override
    protected boolean isGraph() {
        return true;
    }

    @Override
    protected void initRepo() {

        final TsQuarkusExt coreExt = new TsQuarkusExt("test-core-ext");

        var tomatoExt = new TsQuarkusExt("quarkus-tomato").addDependency(coreExt);
        var mozzarellaExt = new TsQuarkusExt("quarkus-mozzarella").addDependency(coreExt);
        var basilExt = new TsQuarkusExt("quarkus-basil").addDependency(coreExt);

        var oilJar = TsArtifact.jar("quarkus-oil");

        var capreseExt = new TsQuarkusExt("quarkus-caprese")
                .setDependencyCondition(tomatoExt, mozzarellaExt, basilExt)
                .addDependency(coreExt);
        capreseExt.getDeployment().addDependency(oilJar);
        capreseExt.install(repoBuilder);

        var saladExt = new TsQuarkusExt("quarkus-salad")
                .setConditionalDeps(capreseExt)
                .addDependency(coreExt);

        app = TsArtifact.jar("app-with-conditional-graph")
                .addDependency(tomatoExt)
                .addDependency(mozzarellaExt)
                .addDependency(basilExt)
                .addDependency(saladExt)
                .addDependency(oilJar);

        appModel = app.getPomModel();
        app.install(repoBuilder);
    }
}

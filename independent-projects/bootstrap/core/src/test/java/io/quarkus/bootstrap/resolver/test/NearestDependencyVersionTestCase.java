package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.DependencyFlags;

public class NearestDependencyVersionTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        TsArtifact a = new TsArtifact("a");
        TsArtifact b1 = new TsArtifact("b", "1");
        TsArtifact b2 = new TsArtifact("b", "2");
        TsArtifact c = new TsArtifact("c");
        TsArtifact d = new TsArtifact("d");
        TsArtifact e = new TsArtifact("e");
        TsArtifact f = new TsArtifact("f");

        a.addDependency(c);
        c.addDependency(b2);
        c.addManagedDependency(new TsDependency(b2).exclude("f"));
        d.addDependency(b1);
        d.addManagedDependency(new TsDependency(b1).exclude("e"));
        b1.addDependency(e);
        b1.addDependency(f);
        b2.addDependency(e);
        b2.addDependency(f);

        installAsDep(a);
        install(b1);
        install(b2);
        install(c);
        install(e);
        install(f);
        installAsDep(d);

        addCollectedDep(c, DependencyFlags.RUNTIME_CP);
        addCollectedDep(b1, DependencyFlags.RUNTIME_CP);
        addCollectedDep(f, DependencyFlags.RUNTIME_CP);
    }
}

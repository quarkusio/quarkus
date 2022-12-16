package io.quarkus.bootstrap.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;

public class TsQuarkusExt {

    protected final TsArtifact runtime;
    protected final TsArtifact deployment;
    protected final List<TsQuarkusExt> extDeps = new ArrayList<>(0);
    protected final TsJar rtContent;
    protected final PropsBuilder rtDescr = PropsBuilder.newInstance();
    private boolean installed;
    private Map<Integer, List<ArtifactKey>> flags = new HashMap<>();

    public TsQuarkusExt(String artifactId) {
        this(artifactId, TsArtifact.DEFAULT_VERSION);
    }

    public TsQuarkusExt(String artifactId, String version) {
        runtime = TsArtifact.jar(artifactId, version);
        deployment = TsArtifact.jar(artifactId + "-deployment", version);
        deployment.addDependency(runtime);
        rtContent = new TsJar();
        runtime.setContent(rtContent);
        rtDescr.set(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment.toString());
    }

    public TsQuarkusExt setDependencyFlag(ArtifactKey dep, int flag) {
        this.flags.computeIfAbsent(flag, k -> new ArrayList<>()).add(dep);
        return this;
    }

    public TsQuarkusExt setConditionalDeps(TsQuarkusExt... exts) {
        final StringBuilder buf = new StringBuilder();
        int i = 0;
        buf.append(exts[i++].getRuntime().toString());
        while (i < exts.length) {
            buf.append(' ').append(exts[i++].getRuntime().toString());
        }
        return setDescriptorProp(BootstrapConstants.CONDITIONAL_DEPENDENCIES, buf.toString());
    }

    public TsQuarkusExt setDependencyCondition(TsQuarkusExt... exts) {
        final StringBuilder buf = new StringBuilder();
        int i = 0;
        buf.append(exts[i++].getRuntime().getKey());
        while (i < exts.length) {
            buf.append(' ').append(exts[i++].getRuntime().getKey());
        }
        return setDescriptorProp(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
    }

    public TsQuarkusExt setDescriptorProp(String name, String value) {
        rtDescr.set(name, value);
        return this;
    }

    public TsQuarkusExt setProvidesCapabilities(String... capability) {
        setDescriptorProp(BootstrapConstants.PROP_PROVIDES_CAPABILITIES,
                Arrays.asList(capability).stream()
                        .collect(Collectors.joining(",")));
        return this;
    }

    public TsArtifact getRuntime() {
        return runtime;
    }

    public TsArtifact getDeployment() {
        return deployment;
    }

    public TsQuarkusExt addDependency(TsQuarkusExt ext, TsArtifact... exclusions) {
        extDeps.add(ext);
        runtime.addDependency(ext.runtime, exclusions);
        deployment.addDependency(ext.deployment);
        return this;
    }

    public void install(TsRepoBuilder repo) {
        if (installed) {
            return;
        }
        installed = true;

        if (!extDeps.isEmpty()) {
            for (TsQuarkusExt e : extDeps) {
                e.install(repo);
            }
        }
        for (Map.Entry<Integer, List<ArtifactKey>> e : flags.entrySet()) {
            switch (e.getKey()) {
                case DependencyFlags.CLASSLOADER_PARENT_FIRST:
                    final StringJoiner sj = new StringJoiner(",");
                    for (ArtifactKey k : e.getValue()) {
                        sj.add(k.toString());
                    }
                    rtDescr.set(BootstrapConstants.PARENT_FIRST_ARTIFACTS, sj.toString());
                    break;
                default:
                    throw new RuntimeException("Not yet supported flag " + e.getKey());
            }
        }
        rtContent.addEntry(rtDescr.build(), BootstrapConstants.DESCRIPTOR_PATH);
        deployment.install(repo);
        runtime.install(repo);
    }
}

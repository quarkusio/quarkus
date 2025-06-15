package io.quarkus.maven.components;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.Deployer;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContextConfig;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.MavenModelBuilder;
import io.smallrye.beanbag.BeanSupplier;
import io.smallrye.beanbag.DependencyFilter;
import io.smallrye.beanbag.Scope;
import io.smallrye.beanbag.maven.MavenFactory;

@Singleton
@Named
public class QuarkusWorkspaceProvider {

    private final VersionResolver versionResolver;

    private final VersionRangeResolver versionRangeResolver;

    private final ArtifactResolver artifactResolver;

    private final MetadataResolver metadataResolver;

    private final Deployer deployer;

    private final RemoteRepositoryManager remoteRepoManager;

    private final SettingsDecrypter settingsDecrypter;

    private volatile BootstrapMavenContext ctx;

    @Inject
    public QuarkusWorkspaceProvider(VersionResolver versionResolver, VersionRangeResolver versionRangeResolver,
            ArtifactResolver artifactResolver, MetadataResolver metadataResolver, Deployer deployer,
            RemoteRepositoryManager remoteRepoManager, SettingsDecrypter settingsDecrypter) {
        this.versionResolver = versionResolver;
        this.versionRangeResolver = versionRangeResolver;
        this.artifactResolver = artifactResolver;
        this.metadataResolver = metadataResolver;
        this.deployer = deployer;
        this.remoteRepoManager = remoteRepoManager;
        this.settingsDecrypter = settingsDecrypter;
    }

    public BootstrapMavenContext getMavenContext() {
        return ctx == null ? ctx = createMavenContext(BootstrapMavenContext.config()) : ctx;
    }

    public RepositorySystem getRepositorySystem() {
        try {
            return getMavenContext().getRepositorySystem();
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize Maven repository system", e);
        }
    }

    public RemoteRepositoryManager getRemoteRepositoryManager() {
        return remoteRepoManager;
    }

    public BootstrapMavenContext createMavenContext(BootstrapMavenContextConfig<?> config) {
        try {
            return new BootstrapMavenContext(config) {
                @Override
                protected MavenFactory configureMavenFactory() {
                    final BootstrapMavenContext ctx = this;
                    return MavenFactory.create(
                            List.of(RepositorySystem.class.getClassLoader(), getClass().getClassLoader()),
                            builder -> builder.addBeanInstance(versionResolver).addBeanInstance(versionRangeResolver)
                                    .addBeanInstance(artifactResolver).addBeanInstance(metadataResolver)
                                    .addBeanInstance(deployer).addBeanInstance(remoteRepoManager)
                                    .addBeanInstance(settingsDecrypter).addBean(ModelBuilder.class)
                                    .setSupplier(new BeanSupplier<ModelBuilder>() {
                                        @Override
                                        public ModelBuilder get(Scope scope) {
                                            return new MavenModelBuilder(ctx);
                                        }
                                    }).setPriority(100).build(),
                            DependencyFilter.ACCEPT);
                }
            };
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize Quarkus Maven context", e);
        }
    }

    public MavenArtifactResolver createArtifactResolver(BootstrapMavenContextConfig<?> config) {
        try {
            return new MavenArtifactResolver(createMavenContext(config));
        } catch (BootstrapMavenException e) {
            throw new RuntimeException("Failed to initialize Maven artifact resolver", e);
        }
    }
}

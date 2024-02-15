package io.quarkus.cli.core;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        io.quarkus.devtools.codestarts.core.CodestartSpec.class,
        io.quarkus.devtools.codestarts.core.CodestartSpec.LanguageSpec.class,
        io.quarkus.devtools.codestarts.core.CodestartSpec.CodestartDep.class,
        io.quarkus.registry.catalog.Category.class,
        io.quarkus.registry.catalog.Extension.class,
        io.quarkus.registry.catalog.ExtensionCatalog.class,
        io.quarkus.registry.catalog.ExtensionOrigin.class,
        org.apache.maven.repository.internal.DefaultArtifactDescriptorReader.class,
        org.apache.maven.repository.internal.DefaultVersionRangeResolver.class,
        org.apache.maven.repository.internal.DefaultVersionResolver.class,
        org.apache.maven.repository.internal.SnapshotMetadataGeneratorFactory.class,
        org.apache.maven.repository.internal.VersionsMetadataGeneratorFactory.class,
        org.apache.maven.wagon.providers.http.HttpWagon.class,
        org.apache.maven.wagon.shared.http.AbstractHttpClientWagon.class,
        org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory.class,
        org.eclipse.aether.internal.impl.DefaultArtifactResolver.class,
        org.eclipse.aether.internal.impl.DefaultChecksumPolicyProvider.class,
        org.eclipse.aether.internal.impl.DefaultDeployer.class,
        org.eclipse.aether.internal.impl.DefaultFileProcessor.class,
        org.eclipse.aether.internal.impl.DefaultInstaller.class,
        org.eclipse.aether.internal.impl.DefaultLocalRepositoryProvider.class,
        org.eclipse.aether.internal.impl.DefaultMetadataResolver.class,
        org.eclipse.aether.internal.impl.DefaultOfflineController.class,
        org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager.class,
        org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider.class,
        org.eclipse.aether.internal.impl.DefaultRepositoryEventDispatcher.class,
        org.eclipse.aether.internal.impl.DefaultRepositoryLayoutProvider.class,
        org.eclipse.aether.internal.impl.DefaultRepositorySystem.class,
        org.eclipse.aether.internal.impl.DefaultTransporterProvider.class,
        org.eclipse.aether.internal.impl.DefaultUpdateCheckManager.class,
        org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer.class,
        org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory.class,
        org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory.class,
        org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory.class,
        org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector.class,
        org.eclipse.aether.transport.wagon.WagonTransporterFactory.class
}, ignoreNested = true)
public class Reflections {
}

package io.quarkus.registry;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.maven.MavenRegistryClientFactory;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.client.spi.RegistryClientFactoryProvider;
import io.quarkus.registry.config.RegistryConfig;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.eclipse.aether.artifact.DefaultArtifact;

class RegistryClientFactoryResolver {

    private RegistryClientFactory defaultClientFactory;
    private RegistryClientEnvironment clientEnv;

    final MavenArtifactResolver artifactResolver;
    final MessageWriter log;

    RegistryClientFactoryResolver(MavenArtifactResolver artifactResolver, MessageWriter log) {
        this.artifactResolver = artifactResolver;
        this.log = log;
    }

    RegistryClientFactory getClientFactory(RegistryConfig config) {
        if (config.getExtra().isEmpty()) {
            return getDefaultClientFactory();
        }
        Object provider = config.getExtra().get("client-factory-artifact");
        if (provider != null) {
            return loadFromArtifact(config, provider);
        }
        provider = config.getExtra().get("client-factory-url");
        if (provider != null) {
            final URL url;
            try {
                url = new URL((String) provider);
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to translate " + provider + " to URL", e);
            }
            return loadFromUrl(url);
        }
        return getDefaultClientFactory();
    }

    RegistryClientFactory loadFromArtifact(RegistryConfig config, final Object providerValue) {
        ArtifactCoords providerArtifact = null;
        try {
            final String providerStr = (String) providerValue;
            providerArtifact = ArtifactCoords.fromString(providerStr);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process configuration of " + config.getId()
                    + " registry: failed to cast " + providerValue + " to String", e);
        }
        final File providerJar;
        try {
            providerJar = artifactResolver.resolve(new DefaultArtifact(providerArtifact.getGroupId(),
                    providerArtifact.getArtifactId(), providerArtifact.getClassifier(),
                    providerArtifact.getType(), providerArtifact.getVersion())).getArtifact().getFile();
        } catch (BootstrapMavenException e) {
            throw new IllegalStateException(
                    "Failed to resolve the registry client factory provider artifact " + providerArtifact, e);
        }
        log.debug("Loading registry client factory for %s from %s", config.getId(), providerArtifact);
        final URL url;
        try {
            url = providerJar.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to translate " + providerJar + " to URL", e);
        }
        return loadFromUrl(url);
    }

    private RegistryClientFactory loadFromUrl(final URL url) {
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader providerCl = new URLClassLoader(new URL[] { url }, originalCl);
            final Iterator<RegistryClientFactoryProvider> i = ServiceLoader
                    .load(RegistryClientFactoryProvider.class, providerCl).iterator();
            if (!i.hasNext()) {
                throw new Exception("Failed to locate an implementation of " + RegistryClientFactoryProvider.class.getName()
                        + " service provider");
            }
            final RegistryClientFactoryProvider provider = i.next();
            if (i.hasNext()) {
                final StringBuilder buf = new StringBuilder();
                buf.append("Found more than one registry client factory provider "
                        + provider.getClass().getName());
                while (i.hasNext()) {
                    buf.append(", ").append(i.next().getClass().getName());
                }
                throw new Exception(buf.toString());
            }
            return provider.newRegistryClientFactory(getClientEnv());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load registry client factory from " + url, e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    private RegistryClientFactory getDefaultClientFactory() {
        return defaultClientFactory == null
                ? defaultClientFactory = new MavenRegistryClientFactory(artifactResolver, log)
                : defaultClientFactory;
    }

    RegistryClientEnvironment getClientEnv() {
        return clientEnv == null
                ? clientEnv = new RegistryClientEnvironment() {
                    @Override
                    public MessageWriter log() {
                        return log;
                    }

                    @Override
                    public MavenArtifactResolver resolver() {
                        return artifactResolver;
                    }
                }
                : clientEnv;
    }
}

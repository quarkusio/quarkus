package io.quarkus.bootstrap.resolver.maven;

import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

import static java.util.Optional.ofNullable;

/**
 * Retrieve mirror for a {@link RemoteRepository} if applicable, and setup proxy if applicable.
 */

class ProxifiedAwareMirrorSelector implements MirrorSelector {
    private final ProxySelector proxySelector;
    private final MirrorSelector wrappedMirrorSelector;

    /**
     * Default constructor.
     *
     * @param wrappedMirrorSelector Use to retrieve mirror for the remoteRepository, may be {@code null}.
     * @param proxySelector
     */
    ProxifiedAwareMirrorSelector(MirrorSelector wrappedMirrorSelector, ProxySelector proxySelector) {
        this.wrappedMirrorSelector = wrappedMirrorSelector;
        this.proxySelector = proxySelector;
    }

    @Override
    public RemoteRepository getMirror(final RemoteRepository remoteRepository) {
        final RemoteRepository remoteRepositoryMirror = ofNullable(wrappedMirrorSelector)
                .map(ms -> ms.getMirror(remoteRepository))
                .orElse(null);
        if (remoteRepositoryMirror == null) {
            // No Mirror, return remoteRepository it-self
            return remoteRepository;
        }

        final Proxy proxy = ofNullable(proxySelector)
                .map(ps -> ps.getProxy(remoteRepositoryMirror))
                .orElse(null);
        if (proxy == null) {
            // No Proxy, return remoteRepositoryMirror
            return remoteRepositoryMirror;
        }

        // Available proxy for mirror, use it
        return new RemoteRepository.Builder(remoteRepositoryMirror)
                .setProxy(proxy)
                .build();
    }
}

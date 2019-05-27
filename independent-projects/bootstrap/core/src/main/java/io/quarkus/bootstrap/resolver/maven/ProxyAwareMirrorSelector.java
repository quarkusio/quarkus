package io.quarkus.bootstrap.resolver.maven;

import org.apache.maven.settings.Mirror;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;

import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Retrieve mirror for a {@link RemoteRepository} if applicable, and setup proxy if applicable.
 */

class ProxyAwareMirrorSelector implements MirrorSelector {
    private final ProxySelector proxySelector;
    private final MirrorSelector wrappedMirrorSelector;

    private static MirrorSelector createMirrorSelector(List<Mirror> mirrors) {
        DefaultMirrorSelector ms = new DefaultMirrorSelector();
        if (mirrors != null) {
            for (Mirror m : mirrors) {
                ms.add(
                        m.getId(),
                        m.getUrl(),
                        m.getLayout(),
                        false,
                        m.getMirrorOf(),
                        m.getMirrorOfLayouts()
                );
            }
        }
        return ms;
    }

    ProxyAwareMirrorSelector(List<Mirror> mirrors, ProxySelector proxySelector) {
        this.wrappedMirrorSelector = createMirrorSelector(mirrors);
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

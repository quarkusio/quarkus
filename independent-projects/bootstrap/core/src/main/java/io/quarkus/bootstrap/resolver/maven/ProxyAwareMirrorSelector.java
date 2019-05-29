package io.quarkus.bootstrap.resolver.maven;

import org.apache.maven.settings.Mirror;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;

import java.util.List;

/**
 * Retrieve mirror for a {@link RemoteRepository} if applicable, and setup proxy if applicable.
 */
class ProxyAwareMirrorSelector implements MirrorSelector {
    private final ProxySelector proxySelector;
    private final MirrorSelector wrappedMirrorSelector;
    private static final ProxySelector NULL_PROXY_SELECTOR = new ProxySelector() {
        @Override
        public Proxy getProxy(RemoteRepository repository) {
            return null;
        }
    };


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
        this.proxySelector = proxySelector == null ? NULL_PROXY_SELECTOR : proxySelector;
    }

    /**
     * Replace repo with it's mirror if applicable, and ensure proxy is set-up if needed.
     *
     * @param remoteRepository Remote repository to enhance
     * @return Mirror or RemoteRepository (if no mirror is applicable) but add proxy on this mirror or RemoteRepository if needed
     */
    @Override
    public RemoteRepository getMirror(final RemoteRepository remoteRepository) {
        RemoteRepository remoteRepositoryResult = wrappedMirrorSelector.getMirror(remoteRepository);
        if (remoteRepositoryResult == null) {
            // No Mirror, take remoteRepository it-self
            remoteRepositoryResult = remoteRepository;
        }

        final Proxy proxy = proxySelector.getProxy(remoteRepositoryResult);
        if (proxy == null) {
            // No Proxy, return remoteRepositoryMirror
            return remoteRepositoryResult;
        }

        // Available proxy, use it
        return new RemoteRepository.Builder(remoteRepositoryResult)
                .setProxy(proxy)
                .build();
    }
}
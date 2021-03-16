package io.quarkus.elasticsearch.restclient.common.runtime.graal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.HttpHost;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.util.Args;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * {@link BasicAuthCache} used in the {@link RestClient} is using
 * serialization which is not supported by GraalVM.
 * <p>
 * We substitute it with an implementation which does not use serialization.
 */
@TargetClass(className = "org.elasticsearch.client.RestClient")
final class Substitute_RestClient {

    @Alias
    private ConcurrentMap<HttpHost, DeadHostState> blacklist;

    @Alias
    private volatile NodeTuple<List<Node>> nodeTuple;

    @Substitute
    public synchronized void setNodes(Collection<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("nodes must not be null or empty");
        }
        AuthCache authCache = new NoSerializationBasicAuthCache();

        Map<HttpHost, Node> nodesByHost = new LinkedHashMap<>();
        for (Node node : nodes) {
            Objects.requireNonNull(node, "node cannot be null");
            // TODO should we throw an IAE if we have two nodes with the same host?
            nodesByHost.put(node.getHost(), node);
            authCache.put(node.getHost(), new BasicScheme());
        }
        this.nodeTuple = new NodeTuple<>(Collections.unmodifiableList(new ArrayList<>(nodesByHost.values())),
                authCache);
        this.blacklist.clear();
    }

    @TargetClass(className = "org.elasticsearch.client.DeadHostState")
    final static class DeadHostState {
    }

    @TargetClass(className = "org.elasticsearch.client.RestClient", innerClass = "NodeTuple")
    final static class NodeTuple<T> {

        @Alias
        NodeTuple(final T nodes, final AuthCache authCache) {
        }
    }

    @Contract(threading = ThreadingBehavior.SAFE)
    private static final class NoSerializationBasicAuthCache implements AuthCache {

        private final Map<HttpHost, AuthScheme> map;
        private final SchemePortResolver schemePortResolver;

        public NoSerializationBasicAuthCache(final SchemePortResolver schemePortResolver) {
            this.map = new ConcurrentHashMap<>();
            this.schemePortResolver = schemePortResolver != null ? schemePortResolver
                    : DefaultSchemePortResolver.INSTANCE;
        }

        public NoSerializationBasicAuthCache() {
            this(null);
        }

        protected HttpHost getKey(final HttpHost host) {
            if (host.getPort() <= 0) {
                final int port;
                try {
                    port = schemePortResolver.resolve(host);
                } catch (final UnsupportedSchemeException ignore) {
                    return host;
                }
                return new HttpHost(host.getHostName(), port, host.getSchemeName());
            } else {
                return host;
            }
        }

        @Override
        public void put(final HttpHost host, final AuthScheme authScheme) {
            Args.notNull(host, "HTTP host");
            if (authScheme == null) {
                return;
            }
            this.map.put(getKey(host), authScheme);
        }

        @Override
        public AuthScheme get(final HttpHost host) {
            Args.notNull(host, "HTTP host");
            return this.map.get(getKey(host));
        }

        @Override
        public void remove(final HttpHost host) {
            Args.notNull(host, "HTTP host");
            this.map.remove(getKey(host));
        }

        @Override
        public void clear() {
            this.map.clear();
        }

        @Override
        public String toString() {
            return this.map.toString();
        }
    }
}

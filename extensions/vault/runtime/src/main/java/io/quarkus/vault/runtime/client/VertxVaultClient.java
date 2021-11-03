package io.quarkus.vault.runtime.client;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static io.quarkus.vault.runtime.client.MutinyVertxClientFactory.createHttpClient;
import static java.util.Collections.emptyMap;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.VaultIOException;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
public class VertxVaultClient implements VaultClient {

    private static final HttpMethod LIST = HttpMethod.valueOf("LIST");

    private static final List<String> ROOT_NAMESPACE_API = Arrays.asList("sys/init", "sys/license", "sys/leader", "sys/health",
            "sys/metrics", "sys/config/state", "sys/host-info", "sys/key-status", "sys/storage", "sys/storage/raft");

    private final Vertx vertx;
    private URL baseUrl;
    private final TlsConfig tlsConfig;
    private final VaultConfigHolder vaultConfigHolder;
    private WebClient webClient;

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public VertxVaultClient(VaultConfigHolder vaultConfigHolder, TlsConfig tlsConfig) {
        this.vaultConfigHolder = vaultConfigHolder;
        this.tlsConfig = tlsConfig;
        this.mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.vertx = Vertx.vertx();
    }

    public void init() {
        VaultBootstrapConfig config = vaultConfigHolder.getVaultBootstrapConfig();
        this.webClient = createHttpClient(vertx, config, tlsConfig);
        this.baseUrl = config.url.orElseThrow(new Supplier<VaultException>() {
            @Override
            public VaultException get() {
                return new VaultException("no vault url provided");
            }
        });
    }

    @PreDestroy
    @Override
    public void close() {
        try {
            if (webClient != null) {
                webClient.close();
            }
        } finally {
            vertx.close();
        }
    }

    // ---

    public <T> T put(String path, String token, Object body, int expectedCode) {
        HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.PUT);
        return exec(request, body, null, expectedCode);
    }

    public <T> T list(String path, String token, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(path, token).method(LIST);
        return exec(request, resultClass);
    }

    public <T> T delete(String path, String token, int expectedCode) {
        HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.DELETE);
        return exec(request, expectedCode);
    }

    public <T> T post(String path, String token, Object body, Class<T> resultClass, int expectedCode) {
        HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.POST);
        return exec(request, body, resultClass, expectedCode);
    }

    public <T> T post(String path, String token, Object body, Class<T> resultClass) {
        return post(path, token, emptyMap(), body, resultClass);
    }

    public <T> T post(String path, String token, Map<String, String> headers, Object body, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.POST);
        headers.forEach(request::putHeader);
        return exec(request, body, resultClass);
    }

    public <T> T post(String path, String token, Object body, int expectedCode) {
        HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.POST);
        return exec(request, body, null, expectedCode);
    }

    public <T> T put(String path, String token, Object body, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.PUT);
        return exec(request, body, resultClass);
    }

    public <T> T put(String path, Object body, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(path).method(HttpMethod.PUT);
        return exec(request, body, resultClass);
    }

    public <T> T get(String path, String token, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.GET);
        return exec(request, resultClass);
    }

    public <T> T get(String path, Map<String, String> queryParams, Class<T> resultClass) {
        final HttpRequest<Buffer> request = builder(path, queryParams).method(HttpMethod.GET);
        return exec(request, resultClass);
    }

    public Buffer get(String path, String token) {
        final HttpRequest<Buffer> request = builder(path, token).method(HttpMethod.GET);
        final HttpResponse<Buffer> response = request.send().await().atMost(getRequestTimeout());
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throwVaultException(response);
        }
        return response.body();
    }

    public int head(String path) {
        final HttpRequest<Buffer> request = builder(path).method(HttpMethod.HEAD);
        return exec(request);
    }

    public int head(String path, Map<String, String> queryParams) {
        final HttpRequest<Buffer> request = builder(path, queryParams).method(HttpMethod.HEAD);
        return exec(request);
    }

    private <T> T exec(HttpRequest<Buffer> request, Class<T> resultClass) {
        return exec(request, null, resultClass, 200);
    }

    private <T> T exec(HttpRequest<Buffer> request, int expectedCode) {
        return exec(request, null, null, expectedCode);
    }

    private <T> T exec(HttpRequest<Buffer> request, Object body, Class<T> resultClass) {
        return exec(request, body, resultClass, 200);
    }

    private <T> T exec(HttpRequest<Buffer> request, Object body, Class<T> resultClass, int expectedCode) {
        try {
            Uni<HttpResponse<Buffer>> uni = body == null ? request.send()
                    : request.sendBuffer(Buffer.buffer(requestBody(body)));

            HttpResponse<Buffer> response = uni.await().atMost(getRequestTimeout());

            if (response.statusCode() != expectedCode) {
                throwVaultException(response);
            }
            Buffer responseBuffer = response.body();
            if (responseBuffer != null) {
                return resultClass == null ? null : mapper.readValue(responseBuffer.toString(), resultClass);
            } else {
                return null;
            }

        } catch (JsonProcessingException e) {
            throw new VaultException(e);

        } catch (io.smallrye.mutiny.TimeoutException e) {
            // happens if we reach the atMost condition - see UniAwait.atMost(Duration)
            throw new VaultIOException(e);

        } catch (VertxException e) {
            if ("Connection was closed".equals(e.getMessage())) {
                // happens if the connection gets closed (idle timeout, reset by peer, ...)
                throw new VaultIOException(e);
            } else {
                throw e;
            }

        } catch (CompletionException e) {
            if (e.getCause() instanceof ConnectException) {
                // unable to establish connection
                throw new VaultIOException(e);
            } else if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                // timeout on request - see HttpRequest.timeout(long)
                throw new VaultIOException(e);
            } else {
                throw e;
            }
        }
    }

    private Duration getRequestTimeout() {
        return vaultConfigHolder.getVaultBootstrapConfig().readTimeout;
    }

    private int exec(HttpRequest<Buffer> request) {
        return request.send().await().atMost(getRequestTimeout()).statusCode();
    }

    private void throwVaultException(HttpResponse<Buffer> response) {
        String body = null;
        try {
            body = response.body().toString();
        } catch (Exception e) {
            // ignore
        }
        throw new VaultClientException(response.statusCode(), body);
    }

    private HttpRequest<Buffer> builder(String path, String token) {
        HttpRequest<Buffer> request = builder(path);
        if (token != null) {
            request.putHeader(X_VAULT_TOKEN, token);
        }
        Optional<String> namespace = vaultConfigHolder.getVaultBootstrapConfig().enterprise.namespace;
        if (namespace.isPresent() && !isRootNamespaceAPI(path)) {
            request.putHeader(X_VAULT_NAMESPACE, namespace.get());
        }
        return request;
    }

    private boolean isRootNamespaceAPI(String path) {
        return ROOT_NAMESPACE_API.stream().anyMatch(path::startsWith);
    }

    private HttpRequest<Buffer> builder(String path) {
        return webClient.getAbs(getUrl(path).toString());
    }

    private HttpRequest<Buffer> builder(String path, Map<String, String> queryParams) {
        HttpRequest<Buffer> request = builder(path);
        if (queryParams != null) {
            queryParams.forEach(request::addQueryParam);
        }
        return request;
    }

    private String requestBody(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new VaultException(e);
        }
    }

    private URL getUrl(String path) {
        try {
            return new URL(baseUrl, API_VERSION + "/" + path);
        } catch (MalformedURLException e) {
            throw new VaultException(e);
        }
    }
}

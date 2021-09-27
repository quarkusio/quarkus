package io.quarkus.vault.runtime.client;

import java.util.Map;

import io.vertx.mutiny.core.buffer.Buffer;

public interface VaultClient {

    String X_VAULT_TOKEN = "X-Vault-Token";
    String X_VAULT_NAMESPACE = "X-Vault-Namespace";
    String API_VERSION = "v1";

    <T> T put(String path, String token, Object body, int expectedCode);

    <T> T list(String path, String token, Class<T> resultClass);

    <T> T delete(String path, String token, int expectedCode);

    <T> T post(String path, String token, Object body, Class<T> resultClass, int expectedCode);

    <T> T post(String path, String token, Object body, Class<T> resultClass);

    <T> T post(String path, String token, Map<String, String> headers, Object body, Class<T> resultClass);

    <T> T post(String path, String token, Object body, int expectedCode);

    <T> T put(String path, String token, Object body, Class<T> resultClass);

    <T> T put(String path, Object body, Class<T> resultClass);

    <T> T get(String path, String token, Class<T> resultClass);

    <T> T get(String path, Map<String, String> queryParams, Class<T> resultClass);

    Buffer get(String path, String token);

    int head(String path);

    int head(String path, Map<String, String> queryParams);

    void close();
}

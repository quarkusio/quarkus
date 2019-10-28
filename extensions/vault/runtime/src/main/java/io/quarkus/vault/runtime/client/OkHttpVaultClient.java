package io.quarkus.vault.runtime.client;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static io.quarkus.vault.runtime.client.OkHttpClientFactory.createHttpClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelfBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuthBody;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSign;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerify;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBody;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpVaultClient implements VaultClient {

    private static final Logger log = Logger.getLogger(OkHttpVaultClient.class);

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private URL url;
    private ObjectMapper mapper = new ObjectMapper();

    public OkHttpVaultClient(VaultRuntimeConfig serverConfig) {
        this.client = createHttpClient(serverConfig);
        this.url = serverConfig.url.get();
        this.mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public VaultUserPassAuth loginUserPass(String user, String password) {
        VaultUserPassAuthBody body = new VaultUserPassAuthBody(password);
        return post("auth/userpass/login/" + user, null, body, VaultUserPassAuth.class);
    }

    @Override
    public VaultKubernetesAuth loginKubernetes(String role, String jwt) {
        VaultKubernetesAuthBody body = new VaultKubernetesAuthBody(role, jwt);
        return post("auth/kubernetes/login", null, body, VaultKubernetesAuth.class);
    }

    @Override
    public VaultAppRoleAuth loginAppRole(String roleId, String secretId) {
        VaultAppRoleAuthBody body = new VaultAppRoleAuthBody(roleId, secretId);
        return post("auth/approle/login", null, body, VaultAppRoleAuth.class);
    }

    @Override
    public VaultKvSecretV1 getSecretV1(String token, String secretEnginePath, String path) {
        return get(secretEnginePath + "/" + path, token, VaultKvSecretV1.class);
    }

    @Override
    public VaultKvSecretV2 getSecretV2(String token, String secretEnginePath, String path) {
        return get(secretEnginePath + "/data/" + path, token, VaultKvSecretV2.class);
    }

    @Override
    public VaultRenewSelf renewSelf(String token, String increment) {
        VaultRenewSelfBody body = new VaultRenewSelfBody(increment);
        return post("auth/token/renew-self", token, body, VaultRenewSelf.class);
    }

    @Override
    public VaultLookupSelf lookupSelf(String token) {
        return get("auth/token/lookup-self", token, VaultLookupSelf.class);
    }

    @Override
    public VaultLeasesLookup lookupLease(String token, String leaseId) {
        VaultLeasesBody body = new VaultLeasesBody(leaseId);
        return put("sys/leases/lookup", token, body, VaultLeasesLookup.class);
    }

    @Override
    public VaultRenewLease renewLease(String token, String leaseId) {
        VaultLeasesBody body = new VaultLeasesBody(leaseId);
        return put("sys/leases/renew", token, body, VaultRenewLease.class);
    }

    @Override
    public VaultDatabaseCredentials generateDatabaseCredentials(String token, String databaseCredentialsRole) {
        return get("database/creds/" + databaseCredentialsRole, token, VaultDatabaseCredentials.class);
    }

    @Override
    public VaultTransitEncrypt encrypt(String token, String keyName, VaultTransitEncryptBody body) {
        return post("transit/encrypt/" + keyName, token, body, VaultTransitEncrypt.class);
    }

    @Override
    public VaultTransitDecrypt decrypt(String token, String keyName, VaultTransitDecryptBody body) {
        return post("transit/decrypt/" + keyName, token, body, VaultTransitDecrypt.class);
    }

    @Override
    public VaultTransitSign sign(String token, String keyName, String hashAlgorithm,
            VaultTransitSignBody body) {
        String path = "transit/sign/" + keyName + (hashAlgorithm == null ? "" : "/" + hashAlgorithm);
        return post(path, token, body, VaultTransitSign.class);
    }

    @Override
    public VaultTransitVerify verify(String token, String keyName, String hashAlgorithm, VaultTransitVerifyBody body) {
        String path = "transit/verify/" + keyName + (hashAlgorithm == null ? "" : "/" + hashAlgorithm);
        return post(path, token, body, VaultTransitVerify.class);
    }

    @Override
    public VaultTransitEncrypt rewrap(String token, String keyName, VaultTransitRewrapBody body) {
        return post("transit/rewrap/" + keyName, token, body, VaultTransitEncrypt.class);
    }

    // ---

    protected <T> T post(String path, String token, Object body, Class<T> resultClass, int expectedCode) {
        Request request = builder(path, token).post(requestBody(body)).build();
        return exec(request, resultClass, expectedCode);
    }

    protected <T> T post(String path, String token, Object body, Class<T> resultClass) {
        Request request = builder(path, token).post(requestBody(body)).build();
        return exec(request, resultClass);
    }

    protected <T> T put(String path, String token, Object body, Class<T> resultClass) {
        Request request = builder(path, token).put(requestBody(body)).build();
        return exec(request, resultClass);
    }

    protected <T> T get(String path, String token, Class<T> resultClass) {
        Request request = builder(path, token).get().build();
        return exec(request, resultClass);
    }

    private <T> T exec(Request request, Class<T> resultClass) {
        return exec(request, resultClass, 200);
    }

    private <T> T exec(Request request, Class<T> resultClass, int expectedCode) {
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != expectedCode) {
                throwVaultException(response);
            }
            String jsonBody = response.body().string();
            return resultClass == null ? null : mapper.readValue(jsonBody, resultClass);
        } catch (IOException e) {
            throw new VaultException(e);
        }
    }

    private void throwVaultException(Response response) {
        String body = null;
        try {
            body = response.body().string();
        } catch (Exception e) {
            // ignore
        }
        throw new VaultClientException(response.code(), body);
    }

    private Request.Builder builder(String path, String token) {
        Request.Builder builder = new Request.Builder().url(getUrl(path));
        if (token != null) {
            builder.header(X_VAULT_TOKEN, token);
        }
        return builder;
    }

    private RequestBody requestBody(Object body) {
        try {
            return RequestBody.create(JSON, mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new VaultException(e);
        }
    }

    private URL getUrl(String path) {
        try {
            return new URL(this.url, API_VERSION + "/" + path);
        } catch (MalformedURLException e) {
            throw new VaultException(e);
        }
    }

}

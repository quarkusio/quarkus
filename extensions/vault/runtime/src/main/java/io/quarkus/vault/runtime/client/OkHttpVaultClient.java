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
import io.quarkus.vault.runtime.client.dto.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.VaultAppRoleAuthBody;
import io.quarkus.vault.runtime.client.dto.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.VaultKubernetesAuthBody;
import io.quarkus.vault.runtime.client.dto.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.VaultLeasesBody;
import io.quarkus.vault.runtime.client.dto.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.VaultRenewSelfBody;
import io.quarkus.vault.runtime.client.dto.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.VaultUserPassAuthBody;
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

    // ---

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
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throwVaultException(response);
            }
            String jsonBody = response.body().string();
            return mapper.readValue(jsonBody, resultClass);
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

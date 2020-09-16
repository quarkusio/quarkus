package io.quarkus.vault.runtime.client;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static io.quarkus.vault.runtime.client.OkHttpClientFactory.createHttpClient;
import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigData;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthListRolesResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthReadRoleResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthRoleData;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelfBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuthBody;
import io.quarkus.vault.runtime.client.dto.database.VaultDatabaseCredentials;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2Write;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultHealthResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitResponse;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultListPolicyResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultUnwrapBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultWrapResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPGenerateCodeResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPListKeysResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPReadKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPValidateCodeBody;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPValidateCodeResult;
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
import okhttp3.HttpUrl;
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
    private String kubernetesAuthMountPath;
    private ObjectMapper mapper = new ObjectMapper();

    public OkHttpVaultClient(VaultRuntimeConfig serverConfig) {
        this.client = createHttpClient(serverConfig);
        this.url = serverConfig.url.get();
        this.mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.kubernetesAuthMountPath = serverConfig.authentication.kubernetes.authMountPath;
    }

    @Override
    public VaultUserPassAuth loginUserPass(String user, String password) {
        VaultUserPassAuthBody body = new VaultUserPassAuthBody(password);
        return post("auth/userpass/login/" + user, null, body, VaultUserPassAuth.class);
    }

    @Override
    public VaultKubernetesAuth loginKubernetes(String role, String jwt) {
        VaultKubernetesAuthBody body = new VaultKubernetesAuthBody(role, jwt);
        return post(kubernetesAuthMountPath + "/login", null, body, VaultKubernetesAuth.class);
    }

    @Override
    public void createKubernetesAuthRole(String token, String name, VaultKubernetesAuthRoleData body) {
        post(kubernetesAuthMountPath + "/role/" + name, token, body, 204);
    }

    @Override
    public VaultKubernetesAuthReadRoleResult getVaultKubernetesAuthRole(String token, String name) {
        return get(kubernetesAuthMountPath + "/role/" + name, token, VaultKubernetesAuthReadRoleResult.class);
    }

    @Override
    public VaultKubernetesAuthListRolesResult listKubernetesAuthRoles(String token) {
        return list(kubernetesAuthMountPath + "/role", token, VaultKubernetesAuthListRolesResult.class);
    }

    @Override
    public void deleteKubernetesAuthRoles(String token, String name) {
        delete(kubernetesAuthMountPath + "/role/" + name, token, 204);
    }

    @Override
    public void configureKubernetesAuth(String token, VaultKubernetesAuthConfigData config) {
        post(kubernetesAuthMountPath + "/config", token, config, 204);
    }

    @Override
    public VaultKubernetesAuthConfigResult readKubernetesAuthConfig(String token) {
        return get(kubernetesAuthMountPath + "/config", token, VaultKubernetesAuthConfigResult.class);
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
    public void writeSecretV1(String token, String secretEnginePath, String path, Map<String, String> secret) {
        post(secretEnginePath + "/" + path, token, secret, null, 204);
    }

    @Override
    public void writeSecretV2(String token, String secretEnginePath, String path, VaultKvSecretV2WriteBody body) {
        post(secretEnginePath + "/data/" + path, token, body, VaultKvSecretV2Write.class);
    }

    @Override
    public void deleteSecretV1(String token, String secretEnginePath, String path) {
        delete(secretEnginePath + "/" + path, token, 204);
    }

    @Override
    public void deleteSecretV2(String token, String secretEnginePath, String path) {
        delete(secretEnginePath + "/data/" + path, token, 204);
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

    @Override
    public VaultTOTPCreateKeyResult createTOTPKey(String token, String keyName,
            VaultTOTPCreateKeyBody body) {
        String path = "totp/keys/" + keyName;

        // Depending on parameters it might produce an output or not
        if (body.isProducingOutput()) {
            return post(path, token, body, VaultTOTPCreateKeyResult.class, 200);
        } else {
            post(path, token, body, 204);
            return null;
        }
    }

    @Override
    public VaultTOTPReadKeyResult readTOTPKey(String token, String keyName) {
        String path = "totp/keys/" + keyName;
        return get(path, token, VaultTOTPReadKeyResult.class);
    }

    @Override
    public VaultTOTPListKeysResult listTOTPKeys(String token) {
        return list("totp/keys", token, VaultTOTPListKeysResult.class);
    }

    @Override
    public void deleteTOTPKey(String token, String keyName) {
        String path = "totp/keys/" + keyName;
        delete(path, token, 204);
    }

    @Override
    public VaultTOTPGenerateCodeResult generateTOTPCode(String token, String keyName) {
        String path = "totp/code/" + keyName;
        return get(path, token, VaultTOTPGenerateCodeResult.class);
    }

    @Override
    public VaultTOTPValidateCodeResult validateTOTPCode(String token, String keyName,
            String code) {
        String path = "totp/code/" + keyName;
        VaultTOTPValidateCodeBody body = new VaultTOTPValidateCodeBody(code);
        return post(path, token, body, VaultTOTPValidateCodeResult.class);
    }

    @Override
    public int systemHealth(boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = getHealthParams(isStandByOk, isPerfStandByOk);

        return head("sys/health", queryParams);
    }

    @Override
    public VaultHealthResult systemHealthStatus(boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = getHealthParams(isStandByOk, isPerfStandByOk);
        return get("sys/health", queryParams, VaultHealthResult.class);
    }

    @Override
    public VaultSealStatusResult systemSealStatus() {
        return get("sys/seal-status", emptyMap(), VaultSealStatusResult.class);
    }

    @Override
    public VaultInitResponse init(int secretShares, int secretThreshold) {
        VaultInitBody body = new VaultInitBody(secretShares, secretThreshold);
        return put("sys/init", body, VaultInitResponse.class);
    }

    public VaultWrapResult wrap(String token, long ttl, Object object) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Vault-Wrap-TTL", "" + ttl);
        return post("sys/wrapping/wrap", token, headers, object, VaultWrapResult.class);
    }

    @Override
    public <T> T unwrap(String wrappingToken, Class<T> resultClass) {
        return post("sys/wrapping/unwrap", wrappingToken, VaultUnwrapBody.EMPTY, resultClass);
    }

    @Override
    public VaultPolicyResult getPolicy(String token, String name) {
        return get("sys/policy/" + name, token, VaultPolicyResult.class);
    }

    @Override
    public void createUpdatePolicy(String token, String name, VaultPolicyBody body) {
        put("sys/policy/" + name, token, body, 204);
    }

    @Override
    public VaultListPolicyResult listPolicies(String token) {
        return get("sys/policy", token, VaultListPolicyResult.class);
    }

    @Override
    public void deletePolicy(String token, String name) {
        delete("sys/policy/" + name, token, 204);
    }

    // ---

    protected <T> T list(String path, String token, Class<T> resultClass) {
        Request request = builder(path, token).method("LIST", null).build();
        return exec(request, resultClass);
    }

    protected <T> T delete(String path, String token, int expectedCode) {
        Request request = builder(path, token).delete().build();
        return exec(request, expectedCode);
    }

    protected <T> T post(String path, String token, Object body, Class<T> resultClass, int expectedCode) {
        Request request = builder(path, token).post(requestBody(body)).build();
        return exec(request, resultClass, expectedCode);
    }

    protected <T> T post(String path, String token, Object body, Class<T> resultClass) {
        return post(path, token, emptyMap(), body, resultClass);
    }

    protected <T> T post(String path, String token, Map<String, String> headers, Object body, Class<T> resultClass) {
        Request.Builder builder = builder(path, token).post(requestBody(body));
        headers.forEach(builder::header);
        Request request = builder.build();
        return exec(request, resultClass);
    }

    protected <T> T post(String path, String token, Object body, int expectedCode) {
        Request request = builder(path, token).post(requestBody(body)).build();
        return exec(request, expectedCode);
    }

    protected <T> T put(String path, String token, Object body, int expectedCode) {
        Request request = builder(path, token).put(requestBody(body)).build();
        return exec(request, expectedCode);
    }

    protected <T> T put(String path, String token, Object body, Class<T> resultClass) {
        Request request = builder(path, token).put(requestBody(body)).build();
        return exec(request, resultClass);
    }

    protected <T> T put(String path, Object body, Class<T> resultClass) {
        Request request = builder(path).put(requestBody(body)).build();
        return exec(request, resultClass);
    }

    protected <T> T get(String path, String token, Class<T> resultClass) {
        Request request = builder(path, token).get().build();
        return exec(request, resultClass);
    }

    protected <T> T get(String path, Map<String, String> queryParams, Class<T> resultClass) {
        final Request request = builder(path, queryParams).get().build();
        return exec(request, resultClass);
    }

    protected int head(String path) {
        final Request request = builder(path).head().build();
        return exec(request);
    }

    protected int head(String path, Map<String, String> queryParams) {
        final Request request = builder(path, queryParams).head().build();
        return exec(request);
    }

    private <T> T exec(Request request, Class<T> resultClass) {
        return exec(request, resultClass, 200);
    }

    private <T> T exec(Request request, int expectedCode) {
        return exec(request, null, expectedCode);
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

    private int exec(Request request) {
        try (Response response = client.newCall(request).execute()) {
            return response.code();
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

    private Request.Builder builder(String path) {
        Request.Builder builder = new Request.Builder().url(getUrl(path));
        return builder;
    }

    private Request.Builder builder(String path, Map<String, String> queryParams) {
        HttpUrl.Builder httpBuilder = HttpUrl.parse(getUrl(path).toExternalForm()).newBuilder();
        if (queryParams != null) {
            queryParams.forEach((name, value) -> httpBuilder.addQueryParameter(name, value));
        }
        Request.Builder builder = new Request.Builder().url(httpBuilder.build());
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

    private Map<String, String> getHealthParams(boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = new HashMap<>();
        if (isStandByOk) {
            queryParams.put("standbyok", "true");
        }

        if (isPerfStandByOk) {
            queryParams.put("perfstandbyok", "true");
        }

        return queryParams;
    }

}

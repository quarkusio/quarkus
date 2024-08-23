package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(UploadUnlimitedTestCase.UploadLimitProfile.class)
public class UploadUnlimitedTestCase {

    /** See {@link io.quarkus.it.vertx.UploadRoute}. */
    @TestHTTPResource(value = "/unlimited-upload")
    URL unlimitedUrl;

    /** See {@link io.quarkus.it.vertx.UploadRoute}. */
    @TestHTTPResource(value = "/limited-upload")
    URL limitedUrl;

    /** No {@code quarkus.http.limits.max-body-size} via route-config. */
    @Test
    public void uploadBypassBodySizeLimit() throws Exception {
        HttpURLConnection urlConn = (HttpURLConnection) unlimitedUrl.openConnection();
        byte[] justData = new byte[8192];
        urlConn.setRequestMethod("POST");
        urlConn.setDoOutput(true);
        urlConn.setRequestProperty("Content-Type", "multipart/form-data");
        urlConn.setRequestProperty("Content-Length", String.valueOf(justData.length));
        try (OutputStream output = urlConn.getOutputStream()) {
            output.write(justData);
        }
        assertEquals(200, urlConn.getResponseCode(), urlConn.getResponseCode() + ": " + urlConn.getResponseMessage());
    }

    /** Respects {@code quarkus.http.limits.max-body-size}. */
    @Test
    public void uploadSizeLimitedByConfig() throws Exception {
        HttpURLConnection urlConn = (HttpURLConnection) limitedUrl.openConnection();
        byte[] justData = new byte[8192];
        urlConn.setRequestMethod("POST");
        urlConn.setDoOutput(true);
        urlConn.setRequestProperty("Content-Length", String.valueOf(justData.length));
        try (OutputStream output = urlConn.getOutputStream()) {
            output.write(justData);
        }
        assertEquals(413, urlConn.getResponseCode(), urlConn.getResponseCode() + ": " + urlConn.getResponseMessage());
    }

    public static class UploadLimitProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.limits.max-body-size", "1K");
        }
    }
}

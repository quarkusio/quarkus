package io.quarkus.pulsar.runtime.graal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.pulsar.client.impl.auth.oauth2.KeyFile;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.scurrilous.circe.checksum.IntHash;
import com.scurrilous.circe.checksum.Java8IntHash;

@TargetClass(className = "com.scurrilous.circe.checksum.Crc32cIntChecksum")
final class Target_com_scurrilous_circe_checksum_Crc32cIntChecksum {

    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    @Alias
    private static IntHash CRC32C_HASH = new Java8IntHash();

}

@TargetClass(className = "org.apache.pulsar.client.impl.auth.oauth2.ClientCredentialsFlow")
final class Target_org_apache_pulsaR_client_impl_auth_oauth2_ClientCredentialsFlow {

    @Substitute
    private static KeyFile loadPrivateKey(String privateKeyURL) throws IOException {
        try {
            URLConnection urlConnection = new org.apache.pulsar.client.api.url.URL(privateKeyURL).openConnection();
            try {
                String protocol = urlConnection.getURL().getProtocol();
                if ("data".equals(protocol) && !"application/json".equals(urlConnection.getContentType())) {
                    throw new IllegalArgumentException(
                            "Unsupported media type or encoding format: " + urlConnection.getContentType());
                }
                KeyFile privateKey;
                try (Reader r = new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8)) {
                    privateKey = KeyFile.fromJson(r);
                }
                return privateKey;
            } finally {
                IOUtils.close(urlConnection);
            }
        } catch (URISyntaxException | InstantiationException | IllegalAccessException e) {
            throw new IOException("Invalid privateKey format", e);
        }
    }

}

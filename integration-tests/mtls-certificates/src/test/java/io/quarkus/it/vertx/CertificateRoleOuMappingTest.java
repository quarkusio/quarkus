package io.quarkus.it.vertx;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(CertificateRoleOuMappingTest.CertDnOuTestProfile.class)
@QuarkusTest
public class CertificateRoleOuMappingTest extends AbstractCertificateRoleMappingTest {

    public static class CertDnOuTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.auth.certificate-role-properties", "ou-role-mappings.txt",
                    "quarkus.http.auth.certificate-role-attribute", "OU");
        }
    }

}

package io.quarkus.it.vertx;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(CertificateRoleSanOtherNameMappingTest.CertSanTestProfile.class)
@QuarkusTest
public class CertificateRoleSanOtherNameMappingTest extends AbstractCertificateRoleMappingTest {

    public static class CertSanTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.auth.certificate-role-properties", "san-any-role-mappings.txt",
                    "quarkus.http.auth.certificate-role-attribute", "SAN_ANY");
        }
    }
}

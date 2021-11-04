package io.quarkus.vault;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.auth.VaultKubernetesAuthConfig;
import io.quarkus.vault.auth.VaultKubernetesAuthRole;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultAuthITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault.properties", "application.properties"));

    @Inject
    VaultKubernetesAuthService vaultKubernetesAuthService;

    @Test
    public void role() {
        String name = "test-auth-k8s";
        List<String> boundServiceAccountNames = asList("vault-auth");
        List<String> boundServiceAccountNamespaces = asList("default");
        List<String> tokenPolicies = asList("dev", "prod");
        vaultKubernetesAuthService.createRole(name, new VaultKubernetesAuthRole()
                .setBoundServiceAccountNames(boundServiceAccountNames)
                .setBoundServiceAccountNamespaces(boundServiceAccountNamespaces)
                .setTokenPolicies(tokenPolicies));

        List<String> roles = vaultKubernetesAuthService.getRoles();
        assertTrue(roles.contains(name));

        VaultKubernetesAuthRole role = vaultKubernetesAuthService.getRole(name);
        assertEquals(boundServiceAccountNames, role.boundServiceAccountNames);
        assertEquals(boundServiceAccountNamespaces, role.boundServiceAccountNamespaces);
        assertEquals(tokenPolicies, role.tokenPolicies);

        vaultKubernetesAuthService.deleteRole(name);

        roles = vaultKubernetesAuthService.getRoles();
        assertFalse(roles.contains(name));
    }

    @Test
    public void config() {

        String caCert = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDMTCCAhmgAwIBAgIJAM9e7Tsk0nLvMA0GCSqGSIb3DQEBCwUAMEcxCzAJBgNV\n" +
                "BAYTAkZSMRMwEQYDVQQIDApTb21lLVN0YXRlMRIwEAYDVQQKDAlBY21lIENvcnAx\n" +
                "DzANBgNVBAMMBnRlc3RjYTAeFw0yMDA0MDQxMjEyNTlaFw0zMDA0MDIxMjEyNTla\n" +
                "MEcxCzAJBgNVBAYTAkZSMRMwEQYDVQQIDApTb21lLVN0YXRlMRIwEAYDVQQKDAlB\n" +
                "Y21lIENvcnAxDzANBgNVBAMMBnRlc3RjYTCCASIwDQYJKoZIhvcNAQEBBQADggEP\n" +
                "ADCCAQoCggEBANGaRFI/mUr94qKSdOvev/bL8CYx2rw4VmrqMytzH78s+3cSpOYw\n" +
                "ovKY9Ua5+F//5XSuY2oQEf1GUg7a+66awGdzKcqX1+BWU68S2YUKKER5jGrfEvvR\n" +
                "AVQeqMf6U12EG89JHWYlgSsNFNKGp0sckH4PZyZchkQSU7VTQ0o7ZzimVOxhVzmP\n" +
                "7ZuYtRGmFRGCmROlIzzdUJF8/ntygaexxyCq51UZ5ntT22RfenP/NAGReS+1R54x\n" +
                "y6GPu5opxXZ4m7vnJ3B6z/C6nJ/xoZvzd/SdtV6k2u7f4FuBL8c63AhFp8WL5x7b\n" +
                "Ce3weNLmq8uZF2hPYDIp8yIF1M8p53dgCVkCAwEAAaMgMB4wDAYDVR0TBAUwAwEB\n" +
                "/zAOBgNVHQ8BAf8EBAMCAgQwDQYJKoZIhvcNAQELBQADggEBAIfk2XRUBLptRHMI\n" +
                "S7fhUgRFsPbv7audIw2Lg/OaLBGpKuitTe3BJ6dgiSpQGBSaw9cKs0ixolGVOTmU\n" +
                "WrMqVCtxOTzT4Fnoa8VoBDEAP/nvk61gS4tJNz1Xj6/TUHb/Gne6bNWE7uol9C7U\n" +
                "R971oiC1IR6BA5su27R2AcpUNuU5ql4XVgmso2NMxQ7ayKMmo4Mzy8V3rN0KoqdK\n" +
                "vugY1vmMjqdD1aMz2ANUVfhvyQ1UCI2XPEE0R5GJ2EE5dv/YuuDvXrmtqnTEVWoB\n" +
                "4vgsrpPCXiDwzPTogeT09m/fsXealSfNcTN7VcGap7aqtZfepPcyxBwDddQsNjyx\n" +
                "CaRgMW4=\n" +
                "-----END CERTIFICATE-----";

        String kubernetesHost = "https://192.168.99.100:8443";
        vaultKubernetesAuthService.configure(new VaultKubernetesAuthConfig()
                .setKubernetesHost(kubernetesHost)
                .setKubernetesCaCert(caCert));

        VaultKubernetesAuthConfig config = vaultKubernetesAuthService.getConfig();
        assertEquals(kubernetesHost, config.kubernetesHost);
        assertEquals(caCert, config.kubernetesCaCert);
    }
}

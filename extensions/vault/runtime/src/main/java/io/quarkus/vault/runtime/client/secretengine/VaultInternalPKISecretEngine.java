package io.quarkus.vault.runtime.client.secretengine;

import static java.util.Collections.emptyList;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICRLRotateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICertificateListResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCRLBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCRLResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigURLsBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigURLsResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIEnableBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateCertificateBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateCertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateIntermediateCSRBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateIntermediateCSRResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateRootBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateRootResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRevokeCertificateBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRevokeCertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRoleReadResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRoleUpdateBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRolesListData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRolesListResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISetSignedIntermediateCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignCertificateRequestBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignCertificateRequestResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignIntermediateCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKITidyBody;

@Singleton
public class VaultInternalPKISecretEngine extends VaultInternalBase {

    private String getPath(String mount, String path) {
        return mount + "/" + path;
    }

    public VaultPKICertificateResult getCertificate(String token, String mount, String serial) {
        return vaultClient.get(getPath(mount, "cert/" + serial), token, VaultPKICertificateResult.class);
    }

    public VaultPKICertificateListResult listCertificates(String token, String mount) {
        return vaultClient.list(getPath(mount, "certs"), token, VaultPKICertificateListResult.class);
    }

    public void configCertificateAuthority(String token, String mount, VaultPKIConfigCABody body) {
        vaultClient.post(getPath(mount, "config/ca"), token, body, 204);
    }

    public VaultPKICRLRotateResult rotateCertificateRevocationList(String token, String mount) {
        return vaultClient.get(getPath(mount, "crl/rotate"), token, VaultPKICRLRotateResult.class);
    }

    public VaultPKIGenerateCertificateResult generateCertificate(
            String token,
            String mount,
            String role,
            VaultPKIGenerateCertificateBody body) {
        return vaultClient.post(getPath(mount, "issue/" + role), token, body, VaultPKIGenerateCertificateResult.class);
    }

    public VaultPKISignCertificateRequestResult signCertificate(
            String token,
            String mount,
            String role,
            VaultPKISignCertificateRequestBody body) {
        return vaultClient.post(getPath(mount, "sign/" + role), token, body, VaultPKISignCertificateRequestResult.class);
    }

    public VaultPKIRevokeCertificateResult revokeCertificate(String token, String mount,
            VaultPKIRevokeCertificateBody body) {
        return vaultClient.post(getPath(mount, "revoke"), token, body, VaultPKIRevokeCertificateResult.class);
    }

    public void updateRole(String token, String mount, String role, VaultPKIRoleUpdateBody body) {
        vaultClient.post(getPath(mount, "roles/" + role), token, body, 204);
    }

    public VaultPKIRoleReadResult readRole(String token, String mount, String role) {
        return vaultClient.get(getPath(mount, "roles/" + role), token, VaultPKIRoleReadResult.class);
    }

    public VaultPKIRolesListResult listRoles(String token, String mount) {
        try {
            return vaultClient.list(getPath(mount, "roles"), token, VaultPKIRolesListResult.class);
        } catch (VaultClientException x) {
            // Translate 404 to empty list
            if (x.getStatus() == 404) {
                VaultPKIRolesListResult empty = new VaultPKIRolesListResult();
                empty.data = new VaultPKIRolesListData();
                empty.data.keys = emptyList();
                return empty;
            } else {
                throw x;
            }
        }
    }

    public void deleteRole(String token, String mount, String role) {
        vaultClient.delete(getPath(mount, "roles/" + role), token, 204);
    }

    public VaultPKIGenerateRootResult generateRoot(String token, String mount, String type,
            VaultPKIGenerateRootBody body) {
        return vaultClient.post(getPath(mount, "root/generate/" + type), token, body, VaultPKIGenerateRootResult.class);
    }

    public void deleteRoot(String token, String mount) {
        vaultClient.delete(getPath(mount, "root"), token, 204);
    }

    public VaultPKISignCertificateRequestResult signIntermediateCA(String token, String mount,
            VaultPKISignIntermediateCABody body) {
        return vaultClient.post(getPath(mount, "root/sign-intermediate"),
                token,
                body,
                VaultPKISignCertificateRequestResult.class);
    }

    public VaultPKIGenerateIntermediateCSRResult generateIntermediateCSR(String token, String mount, String type,
            VaultPKIGenerateIntermediateCSRBody body) {
        return vaultClient.post(getPath(mount, "intermediate/generate/" + type),
                token,
                body,
                VaultPKIGenerateIntermediateCSRResult.class);
    }

    public void setSignedIntermediateCA(String token, String mount, VaultPKISetSignedIntermediateCABody body) {
        vaultClient.post(getPath(mount, "intermediate/set-signed"), token, body, 204);
    }

    public void tidy(String token, String mount, VaultPKITidyBody body) {
        vaultClient.post(getPath(mount, "tidy"), token, body, 202);
    }

    public void configURLs(String token, String mount, VaultPKIConfigURLsBody body) {
        vaultClient.post(getPath(mount, "config/urls"), token, body, 204);
    }

    public VaultPKIConfigURLsResult readURLs(String token, String mount) {
        return vaultClient.get(getPath(mount, "config/urls"), token, VaultPKIConfigURLsResult.class);
    }

    public void configCRL(String token, String mount, VaultPKIConfigCRLBody body) {
        vaultClient.post(getPath(mount, "config/crl"), token, body, 204);
    }

    public VaultPKIConfigCRLResult readCRL(String token, String mount) {
        return vaultClient.get(getPath(mount, "config/crl"), token, VaultPKIConfigCRLResult.class);
    }

    public void enableEngine(String token, String mount, VaultPKIEnableBody body) {
        vaultClient.post("sys/mounts/" + mount, token, body, 204);
    }

    public void disableEngine(String token, String mount) {
        vaultClient.delete("sys/mounts/" + mount, token, 204);
    }

    public boolean checkEngineEnabled(String token, String mount) {
        try {
            configURLs(token, mount, new VaultPKIConfigURLsBody());
            return true;
        } catch (Throwable x) {
            return false;
        }
    }
}

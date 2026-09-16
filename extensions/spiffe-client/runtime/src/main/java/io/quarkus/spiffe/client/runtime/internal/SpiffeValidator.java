package io.quarkus.spiffe.client.runtime.internal;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.spiffe.client.SpiffeConnectionException;

final class SpiffeValidator {

    private static final String SPIFFE_URI_PREFIX = "spiffe://";
    private static final int URI_SAN_TYPE = 6;
    private static final int MAX_SPIFFE_ID_LENGTH = 2048;
    private static final int MAX_TRUST_DOMAIN_LENGTH = 255;

    private SpiffeValidator() {
    }

    static String validateLeaf(X509Certificate leaf) throws SpiffeConnectionException {
        if (leaf.getBasicConstraints() != -1) {
            throw new SpiffeConnectionException("Leaf certificate must not have CA flag set to true");
        }

        boolean[] keyUsage = leaf.getKeyUsage();
        if (keyUsage == null) {
            throw new SpiffeConnectionException("Leaf certificate is missing the key usage extension");
        }
        if (keyUsage.length < 1 || !keyUsage[0]) {
            throw new SpiffeConnectionException("Leaf certificate must have 'digitalSignature' as key usage");
        }
        if (keyUsage.length > 5 && keyUsage[5]) {
            throw new SpiffeConnectionException("Leaf certificate must not have 'keyCertSign' as key usage");
        }
        if (keyUsage.length > 6 && keyUsage[6]) {
            throw new SpiffeConnectionException("Leaf certificate must not have 'cRLSign' as key usage");
        }

        return extractAndValidateUriSan(leaf);
    }

    // X.509-SVID 3.2 SHOULD: signing cert SHOULD itself be an SVID (not enforced — upstream CA may not be SPIFFE-aware)
    // X.509-SVID 3.2 SHOULD: signing cert SHOULD reside in the trust domain of leaf SVIDs it issues (not enforced — cross-domain signing is allowed)
    static void validateIntermediate(X509Certificate cert) throws SpiffeConnectionException {
        if (cert.getBasicConstraints() < 0) {
            throw new SpiffeConnectionException(
                    "Signing certificate must have CA flag set to true: " + cert.getSubjectX500Principal());
        }
        boolean[] keyUsage = cert.getKeyUsage();
        if (keyUsage == null || keyUsage.length <= 5 || !keyUsage[5]) {
            throw new SpiffeConnectionException(
                    "Signing certificate must have 'keyCertSign' as key usage: " + cert.getSubjectX500Principal());
        }
        // X.509-SVID 3.2 MUST: if signing cert has a SPIFFE ID, it must not have a path component
        String uriSan = extractOptionalUriSan(cert);
        if (uriSan != null && uriSan.startsWith(SPIFFE_URI_PREFIX)) {
            URI uri = URI.create(uriSan);
            String path = uri.getPath();
            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                throw new SpiffeConnectionException(
                        "Signing certificate SPIFFE ID must not have a path component: " + uriSan);
            }
        }
    }

    static void validateSpiffeId(String spiffeId) throws SpiffeConnectionException {
        if (spiffeId == null || spiffeId.isEmpty()) {
            throw new SpiffeConnectionException("SPIFFE ID must not be empty");
        }
        if (spiffeId.length() > MAX_SPIFFE_ID_LENGTH) {
            throw new SpiffeConnectionException("SPIFFE ID exceeds maximum length of " + MAX_SPIFFE_ID_LENGTH
                    + " bytes: " + spiffeId.length());
        }

        if (!spiffeId.startsWith(SPIFFE_URI_PREFIX)) {
            throw new SpiffeConnectionException("SPIFFE ID must have 'spiffe://' scheme: " + spiffeId);
        }
        if (spiffeId.contains("%")) {
            throw new SpiffeConnectionException("SPIFFE ID must not contain percent-encoded characters: " + spiffeId);
        }

        URI uri;
        try {
            uri = URI.create(spiffeId);
        } catch (IllegalArgumentException e) {
            throw new SpiffeConnectionException("SPIFFE ID is not a valid URI: " + spiffeId, e);
        }

        if (uri.getUserInfo() != null) {
            throw new SpiffeConnectionException("SPIFFE ID must not contain userinfo: " + spiffeId);
        }
        if (uri.getPort() != -1) {
            throw new SpiffeConnectionException("SPIFFE ID must not contain a port: " + spiffeId);
        }
        if (uri.getQuery() != null) {
            throw new SpiffeConnectionException("SPIFFE ID must not contain a query: " + spiffeId);
        }
        if (uri.getFragment() != null) {
            throw new SpiffeConnectionException("SPIFFE ID must not contain a fragment: " + spiffeId);
        }

        String trustDomain = uri.getHost();
        if (trustDomain == null || trustDomain.isEmpty()) {
            throw new SpiffeConnectionException("SPIFFE ID must have a non-empty trust domain: " + spiffeId);
        }
        if (trustDomain.length() > MAX_TRUST_DOMAIN_LENGTH) {
            throw new SpiffeConnectionException("SPIFFE ID trust domain exceeds maximum length of "
                    + MAX_TRUST_DOMAIN_LENGTH + " bytes: " + spiffeId);
        }
        for (int i = 0; i < trustDomain.length(); i++) {
            char c = trustDomain.charAt(i);
            if (!isValidTrustDomainChar(c)) {
                throw new SpiffeConnectionException(
                        "SPIFFE ID trust domain contains invalid character '" + c + "': " + spiffeId);
            }
        }

        String path = uri.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            throw new SpiffeConnectionException("SPIFFE ID must have a non-root path: " + spiffeId);
        }
        if (path.endsWith("/")) {
            throw new SpiffeConnectionException("SPIFFE ID path must not have a trailing slash: " + spiffeId);
        }
        String[] segments = path.split("/", -1);
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                throw new SpiffeConnectionException(
                        "SPIFFE ID path must not contain empty segments: " + spiffeId);
            }
            if (".".equals(segment) || "..".equals(segment)) {
                throw new SpiffeConnectionException(
                        "SPIFFE ID path must not contain dot segments: " + spiffeId);
            }
            for (int j = 0; j < segment.length(); j++) {
                char c = segment.charAt(j);
                if (!isValidPathChar(c)) {
                    throw new SpiffeConnectionException(
                            "SPIFFE ID path contains invalid character '" + c + "': " + spiffeId);
                }
            }
        }
    }

    private static String extractOptionalUriSan(X509Certificate cert) {
        try {
            var sans = cert.getSubjectAlternativeNames();
            if (sans == null) {
                return null;
            }
            for (var san : sans) {
                if (san.size() > 1 && san.get(0) instanceof Integer type && type == URI_SAN_TYPE
                        && san.get(1) != null) {
                    return san.get(1).toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractAndValidateUriSan(X509Certificate leaf) throws SpiffeConnectionException {
        try {
            var sans = leaf.getSubjectAlternativeNames();
            if (sans == null || sans.isEmpty()) {
                throw new SpiffeConnectionException("Leaf certificate has no Subject Alternative Names");
            }
            List<String> uriSans = new ArrayList<>();
            for (var san : sans) {
                if (san.size() > 1 && san.get(0) instanceof Integer type && type == URI_SAN_TYPE
                        && san.get(1) != null) {
                    uriSans.add(san.get(1).toString());
                }
            }
            if (uriSans.isEmpty()) {
                throw new SpiffeConnectionException("Leaf certificate has no URI Subject Alternative Names");
            }
            if (uriSans.size() > 1) {
                throw new SpiffeConnectionException(
                        "Leaf certificate must contain exactly one URI SAN, found " + uriSans.size() + ": " + uriSans);
            }
            return uriSans.get(0);
        } catch (SpiffeConnectionException e) {
            throw e;
        } catch (Exception e) {
            throw new SpiffeConnectionException("Failed to extract URI SAN from leaf certificate", e);
        }
    }

    private static boolean isValidTrustDomainChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_';
    }

    private static boolean isValidPathChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '.' || c == '-' || c == '_';
    }
}

package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.COMMON_NAME;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRdnValue;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.security.auth.x500.X500Principal;

import org.jboss.logging.Logger;

import io.vertx.ext.auth.impl.asn.ASN1;

public record CertificateRoleAttribute(Function<X509Certificate, Set<String>> rolesMapper) {

    private static final Logger log = Logger.getLogger(CertificateRoleAttribute.class);
    private static final String SAN_PREFIX = "SAN_";

    CertificateRoleAttribute(String configValue, Map<String, Set<String>> roles) {
        this(of(configValue.toUpperCase(), Map.copyOf(roles)));
    }

    private static Function<X509Certificate, Set<String>> of(String configValue, Map<String, Set<String>> roles) {
        if (configValue.contains(SAN_PREFIX)) {

            return new Function<X509Certificate, Set<String>>() {
                @Override
                public Set<String> apply(X509Certificate certificate) {
                    return extractRolesFromCertSan(certificate, SAN.valueOf(configValue).generalNameType, roles);
                }
            };
        } else {

            return new Function<X509Certificate, Set<String>>() {
                @Override
                public Set<String> apply(X509Certificate certificate) {
                    return extractRolesFromCertRdn(certificate, roles, configValue);
                }
            };
        }
    }

    private static Set<String> extractRolesFromCertRdn(X509Certificate certificate, Map<String, Set<String>> roles,
            String rdnType) {
        X500Principal principal = certificate.getSubjectX500Principal();
        if (principal == null || principal.getName() == null) {
            return Set.of();
        }
        Set<String> matchedRoles;
        if (COMMON_NAME.equals(rdnType)) {
            matchedRoles = roles.get(principal.getName());
            if (matchedRoles != null) {
                return matchedRoles;
            }
        }
        String rdnValue = getRdnValue(principal, rdnType);
        if (rdnValue != null) {
            matchedRoles = roles.get(rdnValue);
            if (matchedRoles != null) {
                return matchedRoles;
            }
        }
        return Set.of();
    }

    private enum SAN {
        /**
         * Subject Alternative Name field Other Name.
         * Please note that only simple case of UTF8 identifier mapping is support.
         * For example, you can map 'other-identifier' to the SecurityIdentity roles.
         * If you use 'openssl' tool, supported Other name definition would look like this:
         * <code>subjectAltName=otherName:1.2.3.4;UTF8:other-identifier</code>
         */
        SAN_ANY(0),
        /**
         * Subject Alternative Name field RFC 822 Name.
         */
        SAN_RFC822(1),
        /**
         * Subject Alternative Name field Uniform Resource Identifier (URI).
         */
        SAN_URI(6);

        private final int generalNameType;

        SAN(int generalNameType) {
            this.generalNameType = generalNameType;
        }
    }

    private static Set<String> extractRolesFromCertSan(X509Certificate certificate, int generalNameType,
            Map<String, Set<String>> roles) {
        final Set<String> result = new HashSet<>();
        try {
            var sanList = certificate.getSubjectAlternativeNames();
            if (sanList != null && !sanList.isEmpty()) {
                for (List<?> objects : sanList) {
                    if (objects != null && objects.size() >= 2) {
                        if (objects.get(0) instanceof Integer thatGeneralNameType) {
                            if (thatGeneralNameType == generalNameType) {

                                // special handling for Other name
                                if (thatGeneralNameType == 0 && objects.get(1) instanceof byte[] byteArr) {
                                    var asn1 = ASN1.parseASN1(byteArr);
                                    if (asn1.is(ASN1.SEQUENCE) && asn1.length() == 2) {

                                        var otherIdentifier = asn1.object(1);
                                        while (otherIdentifier.length() == 1
                                                && otherIdentifier.is(ASN1.CONTEXT_SPECIFIC)) {
                                            // there can be one extra context specific ASN with OpenJDK 17, hence loop
                                            otherIdentifier = otherIdentifier.object(0);
                                        }

                                        if (otherIdentifier.is(ASN1.UTF8_STRING)) {
                                            var value = new String(otherIdentifier.binary(0), StandardCharsets.UTF_8);
                                            if (roles.containsKey(value)) {
                                                result.addAll(roles.get(value));
                                                break;
                                            }
                                        }
                                    }
                                }

                                for (int i = 1; i < objects.size(); i++) {
                                    if (objects.get(i) instanceof String name) {
                                        if (roles.containsKey(name)) {
                                            result.addAll(roles.get(name));
                                        }
                                    }
                                }
                            }
                            continue;
                        }
                    }
                    log.tracef("Cannot map SecurityIdentity roles from '%s' due to unsupported format", objects);
                    break;
                }
            }
        } catch (CertificateParsingException e) {
            log.tracef("Cannot map SecurityIdentity roles as certificate parsing failed");
        }
        return Set.copyOf(result);
    }
}

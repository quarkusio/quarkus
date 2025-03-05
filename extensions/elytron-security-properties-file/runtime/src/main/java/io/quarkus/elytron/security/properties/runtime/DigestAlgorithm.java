package io.quarkus.elytron.security.properties.runtime;

import org.wildfly.security.password.interfaces.DigestPassword;

public enum DigestAlgorithm {

    DIGEST_MD5(DigestPassword.ALGORITHM_DIGEST_MD5),
    DIGEST_SHA(DigestPassword.ALGORITHM_DIGEST_SHA),
    DIGEST_SHA_256(DigestPassword.ALGORITHM_DIGEST_SHA_256),
    DIGEST_SHA_384(DigestPassword.ALGORITHM_DIGEST_SHA_384),
    DIGEST_SHA_512(DigestPassword.ALGORITHM_DIGEST_SHA_512),
    DIGEST_SHA_512_256(DigestPassword.ALGORITHM_DIGEST_SHA_512_256);

    private final String name;

    DigestAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

package io.quarkus.jwt.test;

import java.security.interfaces.RSAPublicKey;

import io.smallrye.jwt.KeyUtils;

import io.quarkus.smallrye.jwt.runtime.auth.PublicKeyProxy;
import io.quarkus.smallrye.jwt.runtime.auth.PublicKeySubstitution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PKSubUnitTest {
    @Test
    public void testSubstitution() throws Exception {
        RSAPublicKey pk = (RSAPublicKey) KeyUtils.readPublicKey("/publicKey.pem");

        PublicKeySubstitution sub = new PublicKeySubstitution();
        PublicKeyProxy proxy = sub.serialize(pk);
        RSAPublicKey check = sub.deserialize(proxy);
        Assertions.assertEquals(pk, check);
    }

}

package org.jboss.shamrock.jwt.test;

import java.security.interfaces.RSAPublicKey;

import org.jboss.shamrock.smallrye.jwt.runtime.auth.PublicKeyProxy;
import org.jboss.shamrock.smallrye.jwt.runtime.auth.PublicKeySubstitution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.jwt.KeyUtils;

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

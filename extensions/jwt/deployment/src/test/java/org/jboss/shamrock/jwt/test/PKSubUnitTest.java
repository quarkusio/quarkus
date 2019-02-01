package org.jboss.shamrock.jwt.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.interfaces.RSAPublicKey;

import io.smallrye.jwt.KeyUtils;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtension;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtensionProxy;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtensionSubstitution;
import org.jboss.shamrock.jwt.runtime.auth.PublicKeyProxy;
import org.jboss.shamrock.jwt.runtime.auth.PublicKeySubstitution;
import org.jboss.shamrock.runtime.ObjectSubstitution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PKSubUnitTest {
    @Test
    public void testSubstitution() throws Exception {
        RSAPublicKey pk = (RSAPublicKey) KeyUtils.readPublicKey("/publicKey.pem");
        System.out.printf("pk, alg: %s, encoding: %s\n", pk.getAlgorithm(), pk.getFormat());

        PublicKeySubstitution sub = new PublicKeySubstitution();
        PublicKeyProxy proxy = sub.serialize(pk);
        RSAPublicKey check = sub.deserialize(proxy);
        Assertions.assertEquals(pk, check);
    }
    @Test
    public void testObjSubTypes() {
        ObjectSubstitution<JWTAuthMethodExtension, JWTAuthMethodExtensionProxy> objSub = new JWTAuthMethodExtensionSubstitution();
        Type[] typeVars = objSub.getClass().getGenericInterfaces();
        Type fromType = ((ParameterizedType)typeVars[0]).getActualTypeArguments()[0];
        Type toType = ((ParameterizedType)typeVars[0]).getActualTypeArguments()[1];
        System.out.printf("from: %s, to: %s\n", fromType, toType);

        Class<? extends ObjectSubstitution<?, ?>> objSubClass =  JWTAuthMethodExtensionSubstitution.class;
        Type[] objClassVars = objSubClass.getGenericInterfaces();
        Class<?> fromClass = (Class<?>) ((ParameterizedType)objClassVars[0]).getActualTypeArguments()[0];
        Class<?> toClass = (Class<?>) ((ParameterizedType)objClassVars[0]).getActualTypeArguments()[1];
        //registerSubstitution(fromClass, toClass, objSubClass);
        //registerSubstitution(objSubClass);
    }

    private <F, T> void registerSubstitution(Class<F> from, Class<T> to, Class<? extends ObjectSubstitution<F, T>> substitution) {

    }
    private <F, T> void registerSubstitution(Class<? extends ObjectSubstitution<F, T>> substitution) {

    }
}

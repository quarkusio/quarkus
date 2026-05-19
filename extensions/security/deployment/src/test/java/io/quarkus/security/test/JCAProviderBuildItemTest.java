package io.quarkus.security.test;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.security.deployment.JCAProviderBuildItem;

public class JCAProviderBuildItemTest {

    @Test
    void singleConfigConstructor() {
        var item = new JCAProviderBuildItem("SunPKCS11");
        Assertions.assertEquals("SunPKCS11", item.getProviderName());
        Assertions.assertTrue(item.getProviderConfigs().isEmpty());
    }

    @Test
    void multipleConfigs() {
        var item = new JCAProviderBuildItem("SunPKCS11",
                List.of("/etc/pkcs11.cfg", "/etc/pkcs11-truststore.cfg"));
        Assertions.assertEquals("SunPKCS11", item.getProviderName());
        Assertions.assertEquals(2, item.getProviderConfigs().size());
        Assertions.assertEquals("/etc/pkcs11.cfg", item.getProviderConfigs().get(0));
        Assertions.assertEquals("/etc/pkcs11-truststore.cfg", item.getProviderConfigs().get(1));
    }

    @Test
    void nullConfigsTreatedAsEmpty() {
        var item = new JCAProviderBuildItem("SunPKCS11", null);
        Assertions.assertEquals("SunPKCS11", item.getProviderName());
        Assertions.assertTrue(item.getProviderConfigs().isEmpty());
    }
}

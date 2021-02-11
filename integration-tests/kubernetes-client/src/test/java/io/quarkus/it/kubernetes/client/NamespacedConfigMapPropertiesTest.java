package io.quarkus.it.kubernetes.client;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTestResource(value = CustomKubernetesMockServerTestResource.class, restrictToAnnotatedClass = true)
@TestProfile(NamespacedConfigMapPropertiesTest.MyProfile.class)
@QuarkusTest
public class NamespacedConfigMapPropertiesTest {

    @Test
    public void testPropertiesReadFromConfigMap() {
        ConfigMapPropertiesTest.assertProperty("dummy", "dummyFromDemo");
        ConfigMapPropertiesTest.assertProperty("someProp1", "val1FromDemo");
        ConfigMapPropertiesTest.assertProperty("someProp2", "val2FromDemo");
        ConfigMapPropertiesTest.assertProperty("someProp3", "val3FromDemo");
        ConfigMapPropertiesTest.assertProperty("someProp4", "val4FromDemo");
        ConfigMapPropertiesTest.assertProperty("someProp5", "val5FromDemo");

        SecretPropertiesTest.assertProperty("dummysecret", "dummysecretFromDemo");
        SecretPropertiesTest.assertProperty("secretProp1", "val1FromDemo");
        SecretPropertiesTest.assertProperty("secretProp2", "val2FromDemo");
        SecretPropertiesTest.assertProperty("secretProp3", "val3FromDemo");
        SecretPropertiesTest.assertProperty("secretProp4", "val4FromDemo");
    }

    public static class MyProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> conf = new HashMap<>();
            conf.put("quarkus.kubernetes-config.enabled", "true");
            conf.put("quarkus.kubernetes-config.config-maps", "cmap3");
            conf.put("quarkus.kubernetes-config.namespace", "demo");
            conf.put("quarkus.kubernetes-config.secrets", "s1");
            conf.put("quarkus.kubernetes-config.secrets.enabled", "true");
            return conf;
        }

    }

}

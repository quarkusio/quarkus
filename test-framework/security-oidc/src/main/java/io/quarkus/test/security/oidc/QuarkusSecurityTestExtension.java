package io.quarkus.test.security.oidc;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.security.common.AbstractSecurityTestExtension;
import io.quarkus.test.security.common.TestSecurityProperties;

public class QuarkusSecurityTestExtension extends AbstractSecurityTestExtension {

    protected TestSecurityProperties getTestSecurityProperties(Class<?> original, Method method) {
        TestOidcSecurity testSecurity = super.getTestSecurityAnnotation(TestOidcSecurity.class, original, method);
        if (testSecurity == null) {
            return null;
        }
        Map<String, String> extraProperties = new HashMap<>();
        if (testSecurity.claims() != null) {
            for (Claim c : testSecurity.claims()) {
                extraProperties.put("claim." + c.key(), c.value());
            }
        }
        if (testSecurity.userinfo() != null) {
            for (UserInfo ui : testSecurity.userinfo()) {
                extraProperties.put("userinfo." + ui.key(), ui.value());
            }
        }
        if (testSecurity.config() != null) {
            for (ConfigMetadata c : testSecurity.config()) {
                extraProperties.put("config." + c.key(), c.value());
            }
        }
        return new TestSecurityProperties(
                testSecurity.user(), testSecurity.roles(), testSecurity.attributes(), testSecurity.authorizationEnabled(),
                extraProperties);
    }
}

package io.quarkus.security.test.rolesallowed;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.security.deployment.DotNames;
import io.quarkus.security.deployment.SecurityProcessor;

public class ConfigExpressionDetectionTest {

    private static final Map<String, String[]> VALID_VALUES;

    static {
        // point here is to verify expected values gathered from @RolesAllowed annotation are detected correctly
        var indexer = new Indexer();
        for (Class<?> aClass : new Class<?>[] { ConfigExpressionDetectionTest.class, ValidValues.class }) {
            final String resourceName = fromClassNameToResourceName(aClass.getName());
            try (InputStream stream = ConfigExpressionDetectionTest.class.getClassLoader()
                    .getResourceAsStream(resourceName)) {
                assert stream != null;
                indexer.index(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        var index = indexer.complete();
        VALID_VALUES = new HashMap<>();
        for (MethodInfo methodInfo : index.getClassByName(DotName.createSimple(ValidValues.class.getName())).methods()) {
            var annotation = methodInfo.annotation(DotNames.ROLES_ALLOWED);
            if (annotation != null) {
                VALID_VALUES.put(methodInfo.name(), annotation.value().asStringArray());
            }
        }
    }

    @Test
    void testConfigExpIsDetected() {
        VALID_VALUES.forEach((methodName, rolesAllowed) -> {
            final int[] expressionPositions;
            switch (methodName) {
                case "secured1":
                    expressionPositions = new ValidValues().secured1();
                    break;
                case "secured2":
                    expressionPositions = new ValidValues().secured2();
                    break;
                case "secured3":
                    expressionPositions = new ValidValues().secured3();
                    break;
                case "secured4":
                    expressionPositions = new ValidValues().secured4();
                    break;
                case "secured5":
                    expressionPositions = new ValidValues().secured5();
                    break;
                default:
                    throw new IllegalStateException();
            }
            Assertions.assertArrayEquals(SecurityProcessor.configExpressionPositions(rolesAllowed),
                    expressionPositions);
        });
    }

    public static final class ValidValues {

        @RolesAllowed({ "first-role", "${second-role}", "third-role" })
        public int[] secured1() {
            return new int[] { 1 };
        }

        @RolesAllowed({ "${first-role}", "second-role", "${third-role}" })
        public int[] secured2() {
            return new int[] { 0, 2 };
        }

        @RolesAllowed({ "${first-role}", "${second-role}", "${third-role: defaultValue}" })
        public int[] secured3() {
            return new int[] { 0, 1, 2 };
        }

        @RolesAllowed({ "first-role", "second-role", "third-role" })
        public int[] secured4() {
            return new int[] {};
        }

        @RolesAllowed("${first-role}, ${second-role: defaultValue}, ${third-role}")
        public int[] secured5() {
            // we expect 1 value as this is considered 1 role
            return new int[] { 0 };
        }
    }

}

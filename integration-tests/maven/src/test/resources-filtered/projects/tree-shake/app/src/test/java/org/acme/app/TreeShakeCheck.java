package org.acme.app;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Integration tests verifying that tree-shaking preserves all classes required at runtime.
 * Each test hits a REST endpoint that exercises a specific reachability path (bytecode references,
 * ServiceLoader, Class.forName, reflection registration, string constants, etc.) and asserts
 * the expected class is available.
 *
 * <p>
 * Named {@code TreeShakeCheck} (not {@code *IT} or {@code *Test}) to avoid being picked up
 * by the outer {@code integration-tests/maven} module's surefire/failsafe scans of
 * {@code target/test-classes}.
 */
@QuarkusIntegrationTest
public class TreeShakeCheck {

    /**
     * Verifies that classes reachable through direct bytecode references (method calls,
     * field access) are preserved.
     */
    @Test
    void testBasic() {
        given().when().get("/tree-shake/basic")
                .then().statusCode(200).body(containsString("ServiceB"));
    }

    /**
     * Verifies that service providers discovered via {@code ServiceLoader.load()} in
     * dependency code are preserved when their loading class becomes reachable.
     */
    @Test
    void testServiceLoader() {
        given().when().get("/tree-shake/serviceloader")
                .then().statusCode(200).body(containsString("ServiceProvider"));
    }

    /**
     * Verifies that classes loaded via {@code Class.forName()} with a string constant
     * are preserved.
     */
    @Test
    void testForName() {
        given().when().get("/tree-shake/forname")
                .then().statusCode(200).body(is("ForNameTarget"));
    }

    /**
     * Verifies that classes loaded via {@code ClassLoader.loadClass()} with a string constant
     * are preserved.
     */
    @Test
    void testLoadClass() {
        given().when().get("/tree-shake/loadclass")
                .then().statusCode(200).body(is("LoadClassTarget"));
    }

    /**
     * Verifies that classes referenced from annotation values (class literals in annotation
     * attributes) are preserved.
     */
    @Test
    void testAnnotations() {
        given().when().get("/tree-shake/annotations")
                .then().statusCode(200).body(is("AnnotationValue"));
    }

    /**
     * Verifies that classes referenced from field-level annotation values
     * (class literals in annotations on fields) are preserved.
     */
    @Test
    void testFieldAnnotations() {
        given().when().get("/tree-shake/field-annotations")
                .then().statusCode(200).body(is("FieldAnnoHolder"));
    }

    /**
     * Verifies that classes appearing only in generic type signatures (e.g. type arguments
     * like {@code Map<String, GenericArg>}) are preserved.
     */
    @Test
    void testGenerics() {
        given().when().get("/tree-shake/generics")
                .then().statusCode(200).body(is("GenericArg"));
    }

    /**
     * Verifies that inner classes and their enclosing outer classes are both preserved,
     * since the JVM resolves the outer class at runtime via {@code Class.getDeclaringClass0()}.
     */
    @Test
    void testInner() {
        given().when().get("/tree-shake/inner")
                .then().statusCode(200).body(is("Outer.Inner"));
    }

    /**
     * Verifies that classes referenced only in method descriptors (parameter types, return types)
     * are preserved.
     */
    @Test
    void testMethodDescriptors() {
        given().when().get("/tree-shake/method-descriptors")
                .then().statusCode(200).body(is("processed:test"));
    }

    /**
     * Verifies that multi-release JAR classes are correctly resolved to the highest version
     * not exceeding the application's target Java version.
     */
    @Test
    void testMultiRelease() {
        given().when().get("/tree-shake/multirelease")
                .then().statusCode(200);
    }

    /**
     * Verifies that JBoss Logging companion classes ({@code _$logger}, {@code _$bundle}, {@code _impl})
     * are preserved when their parent logging class is reachable.
     */
    @Test
    void testJbossLogging() {
        given().when().get("/tree-shake/jboss-logging")
                .then().statusCode(200).body(is("LoggedClass"));
    }

    /**
     * Verifies that exception classes declared in {@code throws} clauses are preserved.
     */
    @Test
    void testThrows() {
        given().when().get("/tree-shake/throws")
                .then().statusCode(200).body(is("ok"));
    }

    /**
     * Verifies that classes referenced only as field types are preserved.
     */
    @Test
    void testFieldTypes() {
        given().when().get("/tree-shake/field-types")
                .then().statusCode(200).body(is("FieldHolder"));
    }

    /**
     * Verifies that classes referenced through invokedynamic bootstrap method handles
     * and arguments (e.g. lambda metafactories) are preserved.
     */
    @Test
    void testInvokeDynamic() {
        given().when().get("/tree-shake/invokedynamic")
                .then().statusCode(200).body(is("test"));
    }

    /**
     * Verifies that sisu named components ({@code META-INF/sisu/javax.inject.Named})
     * are preserved when a reachable class loads the sisu resource via
     * {@code ClassLoader.getResources()}.
     */
    @Test
    void testSisu() {
        given().when().get("/tree-shake/sisu")
                .then().statusCode(200);
    }

    /**
     * Verifies that classes whose bytecode was transformed at build time (e.g. adding
     * interfaces or annotations) are preserved with their transformed bytecode.
     */
    @Test
    void testTransform() {
        given().when().get("/tree-shake/transform")
                .then().statusCode(200).body(is("TransformableClass"));
    }

    /**
     * Verifies that classes registered for reflection via {@code @RegisterForReflection}
     * (producing {@code ReflectiveClassBuildItem}) are preserved as roots.
     */
    @Test
    void testReflection() {
        given().when().get("/tree-shake/reflection")
                .then().statusCode(200).body(is("ReflectionTarget"));
    }

    /**
     * Verifies that classes registered via {@code JniRuntimeAccessBuildItem}
     * are preserved as tree-shaking roots.
     */
    @Test
    void testJni() {
        given().when().get("/tree-shake/jni")
                .then().statusCode(200).body(is("JniTarget"));
    }

    /**
     * Verifies that classes referenced only inside serialized resource files
     * (loaded via {@code ObjectInputStream} from classpath resources) are preserved.
     * The deserialized class has no direct bytecode reference from application code.
     */
    @Test
    void testSerialization() {
        given().when().get("/tree-shake/serialization")
                .then().statusCode(200).body(is("SerializedTarget"));
    }

    /**
     * Verifies that classes dynamically loaded via {@code Class.forName()} with
     * runtime-constructed class names (string concatenation, not constants) are preserved.
     * This exercises the RecordingClassLoader chain analysis.
     */
    @Test
    void testLoadClassChain() {
        given().when().get("/tree-shake/loadclass-chain")
                .then().statusCode(200).body(is("AlphaTarget,BetaTarget"));
    }
}

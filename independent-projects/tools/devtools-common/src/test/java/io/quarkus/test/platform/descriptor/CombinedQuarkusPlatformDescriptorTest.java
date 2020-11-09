package io.quarkus.test.platform.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.dependencies.Category;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.CombinedQuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CombinedQuarkusPlatformDescriptorTest extends PlatformAwareTestBase {

    private static final String DOMINATING_VERSION = "dominating-version";

    QuarkusPlatformDescriptor dominatingPlatform;
    QuarkusPlatformDescriptor defaultPlatform;

    @BeforeEach
    public void setup() {
        dominatingPlatform = new TestDominatingQuarkusPlatformDescriptor();
        defaultPlatform = getPlatformDescriptor();
    }

    @Test
    public void testDominance() throws Exception {

        final QuarkusPlatformDescriptor combined = CombinedQuarkusPlatformDescriptor.builder()
                .addPlatform(dominatingPlatform)
                .addPlatform(defaultPlatform)
                .build();

        final Map<String, Extension> expectedExtensions = toMap(defaultPlatform.getExtensions());
        expectedExtensions.putAll(toMap(dominatingPlatform.getExtensions()));

        assertBom(combined);

        assertEquals(dominatingPlatform.getQuarkusVersion(), combined.getQuarkusVersion());

        assertCategories(combined);

        assertExtensions(expectedExtensions, combined);

        assertEquals("dominating pom.xml template", combined.getTemplate("dir/some-other-file.template"));
        assertEquals(defaultPlatform.getTemplate("dir/some-file.template"),
                combined.getTemplate("dir/some-file.template"));
    }

    private void assertBom(QuarkusPlatformDescriptor descriptor) {
        assertEquals(dominatingPlatform.getBomGroupId(), descriptor.getBomGroupId());
        assertEquals(dominatingPlatform.getBomArtifactId(), descriptor.getBomArtifactId());
        assertEquals(dominatingPlatform.getBomVersion(), descriptor.getBomVersion());
    }

    private void assertCategories(QuarkusPlatformDescriptor descriptor) {
        final List<Category> categories = descriptor.getCategories();
        assertFalse(categories.isEmpty());
        final Map<String, Category> map = categories.stream().collect(Collectors.toMap(Category::getId, c -> c));
        assertEquals("Dominating Web", map.get("web").getName());
        assertEquals("Data", map.get("data").getName());
        assertEquals("Other category", map.get("other").getName());
    }

    private void assertExtensions(Map<String, Extension> expectedExtensions, QuarkusPlatformDescriptor descriptor) {
        final List<Extension> extensions = descriptor.getExtensions();
        assertFalse(extensions.isEmpty());
        for (Extension actual : extensions) {
            final Extension expected = expectedExtensions.get(getGa(actual));
            assertNotNull(expected);
            assertEquals(expected.getVersion(), actual.getVersion());
        }

        final Map<String, Extension> actualMap = toMap(descriptor.getExtensions());
        Extension ext = actualMap.get("io.quarkus:quarkus-jdbc-h2");
        assertNotNull(ext);
        assertEquals(defaultPlatform.getQuarkusVersion(), ext.getVersion());

        ext = actualMap.get("io.quarkus:quarkus-resteasy");
        assertNotNull(ext);
        assertEquals(DOMINATING_VERSION, ext.getVersion());
    }

    private static Map<String, Extension> toMap(final List<Extension> extensions) {
        return extensions.stream().collect(Collectors.toMap(e -> getGa(e), e -> e));
    }

    private static String getGa(Extension e) {
        return e.getGroupId() + ":" + e.getArtifactId();
    }
}

package io.quarkus.devtools.codestarts.extension;

import static io.quarkus.devtools.codestarts.CodestartResourceLoader.loadCodestartsFromResources;
import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.getCodestartResourceLoaders;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.core.GenericCodestartCatalog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class QuarkusExtensionCodestartCatalog extends GenericCodestartCatalog<QuarkusExtensionCodestartProjectInput> {

    public static final String QUARKUS_EXTENSION_CODESTARTS_DIR = "codestarts/quarkus-extension";

    private QuarkusExtensionCodestartCatalog(Collection<Codestart> codestarts) {
        super(codestarts);
    }

    public enum QuarkusExtensionData implements DataKey {
        NAMESPACE_ID("namespace.id"),
        NAMESPACE_NAME("namespace.name"),
        EXTENSION_ID("extension.id"),
        EXTENSION_NAME("extension.name"),
        GROUP_ID("group-id"),
        VERSION("version"),
        PACKAGE_NAME("package-name"),
        CLASS_NAME_BASE("class-name-base"),
        MAVEN_SUREFIRE_PLUGIN_VERSION("maven.surefire-plugin.version"),
        QUARKUS_VERSION("quarkus.version"),
        QUARKUS_BOM_GROUP_ID("quarkus.bom.group-id"),
        QUARKUS_BOM_ARTIFACT_ID("quarkus.bom.artifact-id"),
        QUARKUS_BOM_VERSION("quarkus.bom.version"),
        PROPERTIES_FROM_PARENT("properties.from-parent"),
        PARENT_GROUP_ID("parent.group-id"),
        PARENT_ARTIFACT_ID("parent.artifact-id"),
        PARENT_VERSION("parent.version"),
        PARENT_RELATIVE_PATH("parent.relative-path"),
        IT_PARENT_GROUP_ID("it-parent.group-id"),
        IT_PARENT_ARTIFACT_ID("it-parent.artifact-id"),
        IT_PARENT_VERSION("it-parent.version"),
        IT_PARENT_RELATIVE_PATH("it-parent.relative-path"),
        MAVEN_COMPILER_PLUGIN_VERSION("maven.compiler-plugin-version");

        private final String key;

        QuarkusExtensionData(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }

    public enum Code implements DataKey {
        EXTENSION_BASE,
        QUARKIVERSE,
        DEVMODE_TEST,
        INTEGRATION_TESTS,
        UNIT_TEST
    }

    public enum Tooling implements DataKey {
        GIT
    }

    public static QuarkusExtensionCodestartCatalog fromBaseCodestartsResources()
            throws IOException {
        final Map<String, Codestart> codestarts = loadCodestartsFromResources(getCodestartResourceLoaders(),
                QUARKUS_EXTENSION_CODESTARTS_DIR);
        return new QuarkusExtensionCodestartCatalog(codestarts.values());
    }

    @Override
    protected Collection<Codestart> select(QuarkusExtensionCodestartProjectInput projectInput) {
        projectInput.getSelection().addNames(getCodestarts(projectInput));
        return super.select(projectInput);
    }

    private List<String> getCodestarts(QuarkusExtensionCodestartProjectInput projectInput) {
        final List<String> codestarts = new ArrayList<>();
        codestarts.add(Code.EXTENSION_BASE.key());
        if (!projectInput.withoutDevModeTest()) {
            codestarts.add(Code.DEVMODE_TEST.key());
        }
        if (!projectInput.withoutIntegrationTests()) {
            codestarts.add(Code.INTEGRATION_TESTS.key());
        }
        if (!projectInput.withoutUnitTest()) {
            codestarts.add(Code.UNIT_TEST.key());
        }
        return codestarts;
    }

}

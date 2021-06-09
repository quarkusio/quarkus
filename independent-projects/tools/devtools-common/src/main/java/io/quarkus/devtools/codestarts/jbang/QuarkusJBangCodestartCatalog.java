package io.quarkus.devtools.codestarts.jbang;

import static io.quarkus.devtools.codestarts.CodestartResourceLoader.loadCodestartsFromResources;
import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.getCodestartResourceLoaders;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.core.GenericCodestartCatalog;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class QuarkusJBangCodestartCatalog extends GenericCodestartCatalog<QuarkusJBangCodestartProjectInput> {

    public static final String QUARKUS_JBANG_CODESTARTS_DIR = "codestarts/quarkus-jbang";

    public enum JBangDataKey implements DataKey {
        QUARKUS_BOM_GROUP_ID("quarkus.bom.group-id"),
        QUARKUS_BOM_ARTIFACT_ID("quarkus.bom.artifact-id"),
        QUARKUS_BOM_VERSION("quarkus.bom.version");

        private final String key;

        JBangDataKey(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }

    public enum ExampleCodestart implements DataKey {
        PICOCLI,
        RESTEASY
    }

    public enum ToolingCodestart implements DataKey {
        JBANG_WRAPPER
    }

    private QuarkusJBangCodestartCatalog(Collection<Codestart> codestarts) {
        super(codestarts);
    }

    public static QuarkusJBangCodestartCatalog fromBaseCodestartsResources()
            throws IOException {
        final Map<String, Codestart> codestarts = loadCodestartsFromResources(getCodestartResourceLoaders(),
                QUARKUS_JBANG_CODESTARTS_DIR);
        return new QuarkusJBangCodestartCatalog(codestarts.values());
    }

    public static QuarkusJBangCodestartCatalog fromResourceLoaders(List<ResourceLoader> resourceLoaders)
            throws IOException {
        final Map<String, Codestart> codestarts = loadCodestartsFromResources(resourceLoaders, QUARKUS_JBANG_CODESTARTS_DIR);
        return new QuarkusJBangCodestartCatalog(codestarts.values());
    }

    @Override
    protected Collection<Codestart> select(QuarkusJBangCodestartProjectInput projectInput) {

        if (projectInput.getSelection().getNames().isEmpty()) {
            if (projectInput.getDependencies().stream().anyMatch(s -> s.contains("picocli"))) {
                projectInput.getSelection().addName(ExampleCodestart.PICOCLI.key());
            } else {
                projectInput.getSelection().addName(ExampleCodestart.RESTEASY.key());
            }
        }

        // Add codestarts from extension and for tooling
        projectInput.getSelection().addNames(getToolingCodestarts(projectInput));

        return super.select(projectInput);
    }

    private List<String> getToolingCodestarts(QuarkusJBangCodestartProjectInput projectInput) {
        final List<String> codestarts = new ArrayList<>();
        if (!projectInput.noJBangWrapper()) {
            codestarts.add(ToolingCodestart.JBANG_WRAPPER.key());
        }
        return codestarts;
    }
}

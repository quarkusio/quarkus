package io.quarkus.devtools.codestarts.jbang;

import static io.quarkus.devtools.codestarts.QuarkusPlatformCodestartResourceLoader.platformPathLoader;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartCatalogLoader;
import io.quarkus.devtools.codestarts.CodestartPathLoader;
import io.quarkus.devtools.codestarts.core.GenericCodestartCatalog;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class QuarkusJBangCodestartCatalog extends GenericCodestartCatalog<QuarkusJBangCodestartProjectInput> {

    public static final String QUARKUS_JBANG_CODESTARTS_DIR = "codestarts/quarkus-jbang";

    public enum Tooling implements KeySupplier {
        JBANG_WRAPPER
    }

    private QuarkusJBangCodestartCatalog(Collection<Codestart> codestarts) {
        super(codestarts);
    }

    public static QuarkusJBangCodestartCatalog fromQuarkusPlatformDescriptor(QuarkusPlatformDescriptor platformDescriptor)
            throws IOException {
        final CodestartPathLoader pathLoader = platformPathLoader(platformDescriptor);
        final Collection<Codestart> codestarts = CodestartCatalogLoader.loadCodestarts(pathLoader,
                QUARKUS_JBANG_CODESTARTS_DIR);
        return new QuarkusJBangCodestartCatalog(codestarts);
    }

    @Override
    protected Collection<Codestart> select(QuarkusJBangCodestartProjectInput projectInput) {

        if (projectInput.getDependencies().stream().anyMatch(s -> s.contains("quarkus-picocli"))) {
            projectInput.getSelection().addName("picocli");
        } else {
            projectInput.getSelection().addName("resteasy");
        }

        // Add codestarts from extension and for tooling
        projectInput.getSelection().addNames(getToolingCodestarts(projectInput));

        return super.select(projectInput);
    }

    private List<String> getToolingCodestarts(QuarkusJBangCodestartProjectInput projectInput) {
        final List<String> codestarts = new ArrayList<>();
        if (!projectInput.noJBangWrapper()) {
            codestarts.add(Tooling.JBANG_WRAPPER.getKey());
        }
        return codestarts;
    }

    interface KeySupplier {
        default String getKey() {
            return this.toString().toLowerCase().replace("_", "-");
        }
    }

}

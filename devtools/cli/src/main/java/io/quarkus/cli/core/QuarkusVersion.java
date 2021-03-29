package io.quarkus.cli.core;

import java.nio.file.Paths;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import picocli.CommandLine;

public class QuarkusVersion implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { QuarkusProjectHelper.getProject(Paths.get("").normalize().toAbsolutePath()).getExtensionsCatalog()
                .getQuarkusCoreVersion() };
    }
}

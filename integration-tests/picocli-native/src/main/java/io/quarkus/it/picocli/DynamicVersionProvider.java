package io.quarkus.it.picocli;

import picocli.CommandLine;

public class DynamicVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { "${COMMAND-FULL-NAME} version 1.0" };
    }

}

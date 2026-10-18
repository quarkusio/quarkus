package io.quarkus.deployment.pkg.jar;

import java.nio.file.Path;

interface Decompiler {

    void init(Context context);

    /**
     * @return {@code true} if the decompiler was successfully download or already exists
     */
    boolean downloadIfNecessary();

    /**
     * @return {@code true} if the decompilation process was successful
     */
    boolean decompile(Path jarToDecompile);

    class Context {
        final String versionStr;
        final Path jarLocation;
        final Path decompiledOutputDir;

        public Context(Path jarLocation, Path decompiledOutputDir) {
            this.versionStr = null;
            this.jarLocation = jarLocation;
            this.decompiledOutputDir = decompiledOutputDir;
        }

        public Context(String versionStr, Path jarLocation, Path decompiledOutputDir) {
            this.versionStr = versionStr;
            this.jarLocation = jarLocation;
            this.decompiledOutputDir = decompiledOutputDir;
        }

    }
}

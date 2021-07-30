package io.quarkus.cli.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.CategoryListFormatOptions;
import io.quarkus.cli.common.DebugOptions;
import io.quarkus.cli.common.DevOptions;
import io.quarkus.cli.common.ListFormatOptions;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.cli.registry.RegistryClientMixin;
import io.quarkus.devtools.project.BuildTool;

public class JBangRunner implements BuildSystemRunner {
    static final String[] windowsWrapper = { "jbang.cmd", "jbang.ps1" };
    static final String otherWrapper = "jbang";

    final OutputOptionMixin output;
    final RegistryClientMixin registryClient;
    final PropertiesOptions propertiesOptions;
    final Path projectRoot;

    String mainPath;

    public JBangRunner(OutputOptionMixin output, PropertiesOptions propertiesOptions, RegistryClientMixin registryClient,
            Path projectRoot) {
        this.output = output;
        this.registryClient = registryClient;
        this.projectRoot = projectRoot;
        this.propertiesOptions = propertiesOptions;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.JBANG;
    }

    @Override
    public Integer listExtensionCategories(RunModeOption runMode, CategoryListFormatOptions format) throws Exception {
        throw new UnsupportedOperationException("Not there yet. ;)");
    }

    @Override
    public Integer listExtensions(RunModeOption runMode, ListFormatOptions format, boolean installable, String searchPattern,
            String category)
            throws Exception {
        throw new UnsupportedOperationException("Not there yet. ;)");
    }

    @Override
    public Integer addExtension(RunModeOption runMode, Set<String> extensions) {
        throw new UnsupportedOperationException("Not there yet. ;)");
    }

    @Override
    public Integer removeExtension(RunModeOption runMode, Set<String> extensions) {
        throw new UnsupportedOperationException("Not there yet. ;)");
    }

    @Override
    public BuildCommandArgs prepareBuild(BuildOptions buildOptions, RunModeOption runMode, List<String> params) {
        ArrayDeque<String> args = new ArrayDeque<>();

        if (buildOptions.offline) {
            args.add("--offline");
        }
        if (output.isVerbose()) {
            args.add("--verbose");
        }
        if (buildOptions.buildNative) {
            args.add("--native");
        }
        if (buildOptions.clean) {
            args.add("--fresh");
        }

        args.add("build");
        args.addAll(flattenMappedProperties(propertiesOptions.properties));
        args.add(registryClient.getRegistryClientProperty());
        args.addAll(params);
        args.add(getMainPath());
        return prependExecutable(args);
    }

    @Override
    public List<Supplier<BuildCommandArgs>> prepareDevMode(DevOptions devOptions, DebugOptions debugOptions,
            List<String> params) {
        throw new UnsupportedOperationException("Not there yet. ;)");
    }

    @Override
    public Path getProjectRoot() {
        return projectRoot;
    }

    @Override
    public File getWrapper() {
        return ExecuteUtil.findWrapper(projectRoot, windowsWrapper, otherWrapper);
    }

    @Override
    public File getExecutable() {
        return ExecuteUtil.findExecutable(otherWrapper,
                "Unable to find the jbang executable, is it in your path?",
                output);
    }

    String getMainPath() {
        if (mainPath == null) {
            File src = projectRoot.resolve("src").toFile();
            if (src.exists() && src.isDirectory()) {
                String[] names = src.list();
                if (names != null && names.length > 0) {
                    String first = null;
                    for (String name : names) {
                        if (name.equalsIgnoreCase("main.java")) {
                            mainPath = fixPath(src.toPath().resolve(name));
                            return mainPath;
                        }
                        if (first == null && name.endsWith(".java")) {
                            first = name;
                        }
                    }
                    if (first != null) {
                        mainPath = fixPath(src.toPath().resolve(first));
                        return mainPath;
                    }
                }
            }
            throw new IllegalStateException("Unable to find a source file for use with JBang");
        }
        return mainPath;
    }
}

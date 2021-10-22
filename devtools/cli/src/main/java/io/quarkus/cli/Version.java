package io.quarkus.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.smallrye.common.classloader.ClassPathUtils;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

@CommandLine.Command(name = "version", sortOptions = false, header = "Display version information.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "Options:%n")
public class Version implements CommandLine.IVersionProvider, Callable<Integer> {

    private static String version;

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOption helpOption;

    @CommandLine.ArgGroup(exclusive = false, validate = false)
    protected PropertiesOptions propertiesOptions = new PropertiesOptions();

    @CommandLine.Spec
    CommandSpec spec;

    @CommandLine.Option(order = 3, names = {
            "--dependencies" }, description = "Show project dependency versions")
    boolean dependencies = false;

    @Override
    public Integer call() throws Exception {
        // Gather/interpolate the usual version information via IVersionProvider handling
        output.printText(getVersion());
        return CommandLine.ExitCode.OK;
    }

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { "Client Version " + clientVersion() };
    }

    public static String clientVersion() {
        if (version != null) {
            return version;
        }

        final Properties props = new Properties();
        final URL quarkusPropertiesUrl = Thread.currentThread().getContextClassLoader().getResource("quarkus.properties");
        if (quarkusPropertiesUrl == null) {
            throw new RuntimeException("Failed to locate quarkus.properties on the classpath");
        }

        // we have a special case for file and jar as using getResourceAsStream() on Windows might cause file locks
        if ("file".equals(quarkusPropertiesUrl.getProtocol()) || "jar".equals(quarkusPropertiesUrl.getProtocol())) {
            ClassPathUtils.consumeAsPath(quarkusPropertiesUrl, p -> {
                try (BufferedReader reader = Files.newBufferedReader(p)) {
                    props.load(reader);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load quarkus.properties", e);
                }
            });
        } else {
            try {
                props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("quarkus.properties"));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load quarkus.properties", e);
            }
        }

        version = props.getProperty("quarkus-core-version");
        if (version == null) {
            throw new RuntimeException("Failed to locate quarkus-core-version property in the bundled quarkus.properties");
        }

        return version;
    }
}

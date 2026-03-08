package io.quarkus.cli;

import static io.quarkus.cli.common.VersionHelper.clientVersion;

import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ExitCode;
import io.quarkus.quickcli.VersionProvider;
import io.quarkus.quickcli.annotations.ArgGroup;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Mixin;
import io.quarkus.quickcli.annotations.Spec;

@Command(name = "version", header = "Display CLI version information.", hidden = true)
public class Version implements VersionProvider, Callable<Integer> {

    private static String version;

    @Mixin(name = "output")
    OutputOptionMixin output;

    @Mixin
    HelpOption helpOption;

    @ArgGroup(exclusive = false, validate = false)
    protected PropertiesOptions propertiesOptions = new PropertiesOptions();

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        // Gather/interpolate the usual version information via VersionProvider handling
        output.printText(getVersion());
        return ExitCode.OK;
    }

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { clientVersion() };
    }
}

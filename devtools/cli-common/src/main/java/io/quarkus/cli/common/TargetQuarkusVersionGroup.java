package io.quarkus.cli.common;

import io.quarkus.quickcli.annotations.Option;

public class TargetQuarkusVersionGroup {

    @Option(paramLabel = "targetStream", names = { "-S",
            "--stream" }, description = "A target stream, for example:%n  3.15")
    public String streamId;

    @Option(paramLabel = "targetPlatformVersion", names = { "-P",
            "--platform-version" }, description = "A specific target Quarkus platform version, for example:%n"
                    + "  3.15.2%n")
    public String platformVersion;

    //@Option(names = { "-L",
    //        "--latest" }, description = "Use the latest Quarkus platform version")
    //public boolean latest;
}

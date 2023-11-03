package io.quarkus.cli.update;

import picocli.CommandLine;

public class RewriteGroup {

    @CommandLine.Option(order = 0, names = {
            "--no-rewrite" }, description = "Disable the rewrite feature.", defaultValue = "false")
    public boolean noRewrite = false;

    @CommandLine.Option(order = 1, names = {
            "--dry-run" }, description = "Rewrite in dry-mode.", defaultValue = "false")
    public boolean dryRun = false;

    @CommandLine.Option(order = 2, names = {
            "--update-recipes-version" }, description = "Use a custom io.quarkus:quarkus-update-recipes version. This artifact contains the base recipes used by this tool to update a project.")
    public String updateRecipesVersion;

    @CommandLine.Option(order = 3, names = {
            "--rewrite-plugin-version" }, description = "Use a custom OpenRewrite plugin version.")
    public String pluginVersion;

}

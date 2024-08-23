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
            "--quarkus-update-recipes" }, description = "Use custom io.quarkus:quarkus-update-recipes:LATEST artifact (GAV) or just provide the version. This artifact should contain the base Quarkus update recipes to update a project.")
    public String quarkusUpdateRecipes;

    @CommandLine.Option(order = 3, names = {
            "--rewrite-plugin-version" }, description = "Use a custom OpenRewrite plugin version.")
    public String pluginVersion;

    @CommandLine.Option(order = 4, names = {
            "--additional-update-recipes" }, description = "Specify a list of additional artifacts (GAV) containing rewrite recipes.")
    public String additionalUpdateRecipes;

}

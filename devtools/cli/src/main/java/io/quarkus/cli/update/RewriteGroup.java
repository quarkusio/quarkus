package io.quarkus.cli.update;

import picocli.CommandLine;

public class RewriteGroup {

    @CommandLine.ArgGroup(exclusive = true)
    public RewriteRun run;

    @CommandLine.Option(order = 0, names = {
            "--quarkus-update-recipes" }, description = "Use custom io.quarkus:quarkus-update-recipes:LATEST artifact (GAV) or just provide the version. This artifact should contain the base Quarkus update recipes to update a project.")
    public String quarkusUpdateRecipes;

    @CommandLine.Option(order = 1, names = {
            "--rewrite-plugin-version" }, description = "Use a custom OpenRewrite plugin version.")
    public String pluginVersion;

    @CommandLine.Option(order = 2, names = {
            "--additional-update-recipes" }, description = "Specify a list of additional artifacts (GAV) containing rewrite recipes.")
    public String additionalUpdateRecipes;

    public static class RewriteRun {
        @CommandLine.Option(order = 3, names = {
                "--yes",
                "-y" }, description = "Run the suggested update recipe for this project.", defaultValue = "false")
        public boolean yes = false;

        @CommandLine.Option(order = 5, names = {
                "--dry-run",
                "-n"
        }, description = "Do a dry run of the suggested update recipe for this project and create a patch file.", defaultValue = "false")
        public boolean dryRun = false;

        @CommandLine.Option(order = 4, names = {
                "--no",
                "--no-rewrite",
                "-N" }, description = "Do NOT run the update.", defaultValue = "false")
        public boolean no = false;

    }

}

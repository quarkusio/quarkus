package io.quarkus.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.cli.commands.ListExtensions;
import io.quarkus.cli.commands.QuarkusCommandInvocation;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.gradle.GradleBuildFileFromConnector;

public class QuarkusListExtensions extends QuarkusPlatformTask {

    private boolean all = true;

    private String format = "concise";

    private String searchPattern;

    @Input
    public boolean isAll() {
        return all;
    }

    @Option(description = "List all extensions or just the installable.", option = "all")
    public void setAll(boolean all) {
        this.all = all;
    }

    @Optional
    @Input
    public String getFormat() {
        return format;
    }

    @Option(description = "Select the output format among 'name' (display the name only), 'concise' (display name and description) and 'full' (concise format and version related columns).", option = "format")
    public void setFormat(String format) {
        this.format = format;
    }

    @Optional
    @Input
    public String getSearchPattern() {
        return searchPattern;
    }

    @Option(description = "Search filter on extension list. The format is based on Java Pattern.", option = "searchPattern")
    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    public QuarkusListExtensions() {
        super("Lists the available quarkus extensions");
    }

    @TaskAction
    public void listExtensions() {
        execute();
    }

    @Override
    protected void doExecute(QuarkusCommandInvocation invocation) {
        invocation.setValue(ListExtensions.ALL, isAll())
                .setValue(ListExtensions.FORMAT, getFormat())
                .setValue(ListExtensions.SEARCH, getSearchPattern());
        try {
            new ListExtensions(new GradleBuildFileFromConnector(new FileProjectWriter(getProject().getProjectDir())))
                    .execute(invocation);
        } catch (Exception e) {
            throw new GradleException("Unable to list extensions", e);
        }
    }
}

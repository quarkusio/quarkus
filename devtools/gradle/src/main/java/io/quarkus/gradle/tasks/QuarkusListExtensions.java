package io.quarkus.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.project.QuarkusProject;

public class QuarkusListExtensions extends QuarkusPlatformTask {

    private boolean all = true;
    private boolean installed = false;
    private boolean fromCli = false;

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

    @Input
    public boolean isFromCli() {
        return fromCli;
    }

    @Option(description = "List only installed extensions.", option = "fromCli")
    public void setFromCli(boolean fromCli) {
        this.fromCli = fromCli;
    }

    @Input
    public boolean isInstalled() {
        return installed;
    }

    @Option(description = "List only installed extensions.", option = "installed")
    public void setInstalled(boolean installed) {
        this.installed = installed;
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
        try {
            final QuarkusProject quarkusProject = getQuarkusProject(installed);
            ListExtensions listExtensions = new ListExtensions(quarkusProject)
                    .all(isFromCli() ? false : isAll())
                    .fromCli(isFromCli())
                    .format(getFormat())
                    .installed(isInstalled())
                    .search(getSearchPattern());
            listExtensions.execute();
        } catch (Exception e) {
            throw new GradleException("Unable to list extensions", e);
        }
    }
}

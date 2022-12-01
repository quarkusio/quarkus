package io.quarkus.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.project.QuarkusProject;

public class QuarkusListExtensions extends QuarkusPlatformTask {

    private static final String DEFAULT_FORMAT = "concise";

    private boolean all = true;
    private boolean installed = false;
    private boolean fromCli = false;

    private String format = DEFAULT_FORMAT;

    private String searchPattern;
    private String category;

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

    @Option(description = "Indicates that a task is run from the Quarkus CLI.", option = "fromCli")
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

    @Option(description = "Select the output format among 'id' (display the artifactId only), 'concise' (display name and artifactId) and 'full' (concise format and version related columns).", option = "format")
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

    @Optional
    @Input
    public String getCategory() {
        return category;
    }

    @Option(description = "Only list extensions from given category.", option = "category")
    public void setCategory(String category) {
        this.category = category;
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
                    .search(getSearchPattern())
                    .category(getCategory());
            listExtensions.execute();

            if (!fromCli) {
                GradleMessageWriter log = messageWriter();
                if (DEFAULT_FORMAT.equalsIgnoreCase(format)) {
                    log.info("");
                    log.info(ListExtensions.MORE_INFO_HINT, "--format=full");
                }

                if (!installed && (category == null || category.isBlank())) {
                    log.info("");
                    log.info(ListExtensions.FILTER_HINT, "--category=\"categoryId\"");
                }

                log.info("");
                log.info(ListExtensions.ADD_EXTENSION_HINT,
                        "build.gradle", "./gradlew addExtension --extensions=\"artifactId\"");
            }
        } catch (Exception e) {
            throw new GradleException("Unable to list extensions", e);
        }
    }
}

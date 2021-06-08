package io.quarkus.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.ListCategories;
import io.quarkus.devtools.project.QuarkusProject;

public class QuarkusListCategories extends QuarkusPlatformTask {

    private static final String DEFAULT_FORMAT = "concise";

    private boolean fromCli = false;
    private String format = DEFAULT_FORMAT;

    @Input
    public boolean isFromCli() {
        return fromCli;
    }

    @Option(description = "Indicates that a task is run from the Quarkus CLI.", option = "fromCli")
    public void setFromCli(boolean fromCli) {
        this.fromCli = fromCli;
    }

    @Optional
    @Input
    public String getFormat() {
        return format;
    }

    @Option(description = "Select the output format among 'id' (display the categoryId only), 'concise' (display name and categoryId) and 'full' (name, categoryId and description columns).", option = "format")
    public void setFormat(String format) {
        this.format = format;
    }

    public QuarkusListCategories() {
        super("Lists quarkus extensions categories");
    }

    @TaskAction
    public void listCategories() {
        try {
            final QuarkusProject quarkusProject = getQuarkusProject(false);
            ListCategories listExtensions = new ListCategories(quarkusProject)
                    .fromCli(isFromCli())
                    .format(getFormat());
            listExtensions.execute();

            if (!fromCli) {
                GradleMessageWriter log = messageWriter();

                if (DEFAULT_FORMAT.equalsIgnoreCase(format)) {
                    log.info("");
                    log.info(ListCategories.MORE_INFO_HINT, "--format=full");
                }

                log.info("");
                log.info(ListCategories.LIST_EXTENSIONS_HINT,
                        "`./gradlew listExtensions --category=\"categoryId\"`");
            }
        } catch (Exception e) {
            throw new GradleException("Unable to list extension categories", e);
        }
    }
}

package io.quarkus.runner.bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildExecutionBuilder;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;

/**
 * This phase generates an example configuration file
 *
 * @author Stuart Douglas
 */
public class GenerateConfigTask {

    private static final Logger log = Logger.getLogger(GenerateConfigTask.class);

    private final Path configFile;

    public GenerateConfigTask(Path configFile) {
        this.configFile = configFile;
    }

    public Path run(CuratedApplication application) {
        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        try {
            Path temp = Files.createTempDirectory("empty");
            try {
                AugmentActionImpl augmentAction = new AugmentActionImpl(application, Collections.emptyList());
                BuildResult buildResult = augmentAction.runCustomAction(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder chainBuilder) {
                        chainBuilder.addFinal(ConfigDescriptionBuildItem.class);
                        chainBuilder.addInitial(ArchiveRootBuildItem.class);
                    }
                }, new Consumer<BuildExecutionBuilder>() {
                    @Override
                    public void accept(BuildExecutionBuilder buildExecutionBuilder) {
                        buildExecutionBuilder.produce(new ArchiveRootBuildItem(temp));
                    }
                });

                List<ConfigDescriptionBuildItem> descriptions = buildResult.consumeMulti(ConfigDescriptionBuildItem.class);
                Collections.sort(descriptions);

                String existing = "";
                if (Files.exists(configFile)) {
                    existing = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
                }

                StringBuilder sb = new StringBuilder();
                for (ConfigDescriptionBuildItem i : descriptions) {
                    //we don't want to add these if they already exist
                    //either in commended or uncommented form
                    if (existing.contains("\n" + i.getPropertyName() + "=") ||
                            existing.contains("\n#" + i.getPropertyName() + "=")) {
                        continue;
                    }

                    sb.append("\n#\n");
                    sb.append(formatDocs(i.getDocs()));
                    sb.append("\n#\n#");
                    sb.append(i.getPropertyName() + "=" + i.getDefaultValue());
                    sb.append("\n");
                }

                Files.createDirectories(configFile.getParent());
                Files.write(configFile, sb.toString().getBytes(StandardCharsets.UTF_8),
                        Files.exists(configFile) ? new OpenOption[] { StandardOpenOption.APPEND } : new OpenOption[] {});
            } finally {
                Files.deleteIfExists(temp);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate config file", e);
        }
        return configFile;
    }

    private String formatDocs(String docs) {

        if (docs == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();

        boolean lastEmpty = false;
        boolean first = true;

        for (String line : docs.replace("<p>", "\n").split("\n")) {
            //process line by line
            String trimmed = line.trim();
            //if the lines are empty we only include a single empty line at most, and add a # character
            if (trimmed.isEmpty()) {
                if (!lastEmpty && !first) {
                    lastEmpty = true;
                    builder.append("\n#");
                }
                continue;
            }
            //add the newlines
            lastEmpty = false;
            if (first) {
                first = false;
            } else {
                builder.append("\n");
            }
            //replace some special characters, others are taken care of by regex below
            builder.append("# " + trimmed.replace("\n", "\n#")
                    .replace("<ul>", "")
                    .replace("</ul>", "")
                    .replace("<li>", " - ")
                    .replace("</li>", ""));
        }

        String ret = builder.toString();
        //replace @code
        ret = Pattern.compile("\\{@code (.*?)\\}").matcher(ret).replaceAll("'$1'");
        //replace @link with a reference to the field name
        Matcher matcher = Pattern.compile("\\{@link #(.*?)\\}").matcher(ret);
        while (matcher.find()) {
            ret = ret.replace(matcher.group(0), "'" + configify(matcher.group(1)) + "'");
        }

        return ret;
    }

    private String configify(String group) {
        //replace uppercase characters with a - followed by lowercase
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < group.length(); ++i) {
            char c = group.charAt(i);
            if (Character.isUpperCase(c)) {
                ret.append("-");
                ret.append(Character.toLowerCase(c));
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }
}

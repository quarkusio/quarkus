package io.quarkus.vertx.http.deployment.devmode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.impl.LazyValue;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.vertx.core.json.JsonObject;

public class BuildMetricsDevConsoleProcessor {

    private static final Logger LOG = Logger.getLogger(BuildMetricsDevConsoleProcessor.class.getName());

    @BuildStep(onlyIf = IsDevelopment.class)
    DevConsoleTemplateInfoBuildItem collectMetrics(BuildSystemTargetBuildItem buildSystemTarget) {

        // We need to read the data lazily because the build is not finished yet at the time this build item is produced
        return new DevConsoleTemplateInfoBuildItem("buildMetrics",
                new LazyValue<Map<String, Object>>(new Supplier<Map<String, Object>>() {

                    @Override
                    public Map<String, Object> get() {
                        Map<String, Object> metrics = new HashMap<>();

                        Path metricsJsonFile = buildSystemTarget.getOutputDirectory().resolve("build-metrics.json");
                        if (Files.isReadable(metricsJsonFile)) {
                            try {
                                JsonObject data = new JsonObject(Files.readString(metricsJsonFile));
                                metrics.put("steps", data.getValue("steps"));

                                Set<String> threads = new HashSet<>();
                                for (Object step : data.getJsonArray("steps")) {
                                    threads.add(((JsonObject) step).getString("thread"));
                                }
                                metrics.put("threads", threads);

                            } catch (IOException e) {
                                LOG.error(e);
                            }
                        }
                        return metrics;
                    }
                }));

    }

}

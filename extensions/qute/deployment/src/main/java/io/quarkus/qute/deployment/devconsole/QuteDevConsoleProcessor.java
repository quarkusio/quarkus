package io.quarkus.qute.deployment.devconsole;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.jboss.logging.Logger;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.qute.Variant;
import io.quarkus.qute.deployment.CheckedTemplateBuildItem;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.qute.deployment.TemplateVariantsBuildItem;
import io.quarkus.qute.runtime.devmode.QuteDevConsoleRecorder;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class QuteDevConsoleProcessor {

    private static final Logger LOG = Logger.getLogger(QuteDevConsoleProcessor.class);

    @BuildStep
    @Record(value = STATIC_INIT, optional = true)
    DevConsoleRouteBuildItem invokeEndpoint(QuteDevConsoleRecorder recorder) {
        recorder.setupRenderer();
        return new DevConsoleRouteBuildItem("preview", "POST", new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext context) {
                context.request().setExpectMultipart(true);
                context.request().endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void ignore) {
                        MultiMap form = context.request().formAttributes();
                        String templatePath = form.get("template-select");
                        String testJsonData = form.get("template-data");
                        String contentType = null;
                        String fileName = templatePath;
                        int slashIdx = fileName.lastIndexOf('/');
                        if (slashIdx != -1) {
                            fileName = fileName.substring(slashIdx, fileName.length());
                        }
                        int dotIdx = fileName.lastIndexOf('.');
                        if (dotIdx != -1) {
                            String suffix = fileName.substring(dotIdx + 1, fileName.length());
                            if (suffix.equalsIgnoreCase("json")) {
                                contentType = Variant.APPLICATION_JSON;
                            } else {
                                contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
                            }
                        }
                        try {
                            BiFunction<String, Object, String> renderer = DevConsoleManager
                                    .getGlobal(QuteDevConsoleRecorder.RENDER_HANDLER);
                            Object testData = Json.decodeValue(testJsonData);
                            testData = translate(testData); //translate it to JDK types
                            context.response().setStatusCode(200).putHeader(CONTENT_TYPE, contentType)
                                    .end(renderer.apply(templatePath, testData));
                        } catch (DecodeException e) {
                            context.response().setStatusCode(500).putHeader(CONTENT_TYPE, "text/plain; charset=UTF-8")
                                    .end("Failed to parse JSON: " + e.getMessage());
                        } catch (Throwable e) {
                            context.fail(e);
                        }
                    }
                });
            }
        });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectTemplateInfo(
            List<TemplatePathBuildItem> templatePaths,
            List<CheckedTemplateBuildItem> checkedTemplates,
            TemplateVariantsBuildItem variants) {

        DevQuteInfos quteInfos = new DevQuteInfos();

        for (Entry<String, List<String>> entry : variants.getVariants().entrySet()) {
            CheckedTemplateBuildItem checkedTemplate = findCheckedTemplate(entry.getKey(), checkedTemplates);
            if (checkedTemplate != null) {
                quteInfos.addQuteTemplateInfo(new DevQuteTemplateInfo(checkedTemplate.templateId,
                        processVariants(templatePaths, entry.getValue()),
                        checkedTemplate.method.declaringClass().name() + "." + checkedTemplate.method.name() + "()",
                        checkedTemplate.bindings));
            } else {
                quteInfos.addQuteTemplateInfo(new DevQuteTemplateInfo(entry.getKey(),
                        processVariants(templatePaths, entry.getValue()),
                        null, null));
            }
        }
        return new DevConsoleTemplateInfoBuildItem("devQuteInfos", quteInfos);
    }

    private CheckedTemplateBuildItem findCheckedTemplate(String basePath, List<CheckedTemplateBuildItem> checkedTemplates) {
        for (CheckedTemplateBuildItem checkedTemplate : checkedTemplates) {
            if (checkedTemplate.templateId.equals(basePath)) {
                return checkedTemplate;
            }
        }
        return null;
    }

    private Map<String, String> processVariants(List<TemplatePathBuildItem> templatePaths, List<String> variants) {
        Map<String, String> variantsMap = new HashMap<>();
        for (String variant : variants) {
            String source = "";
            Path sourcePath = templatePaths.stream().filter(p -> p.getPath().equals(variant))
                    .map(TemplatePathBuildItem::getFullPath).findFirst()
                    .orElse(null);
            if (sourcePath != null) {
                try {
                    byte[] content = Files.readAllBytes(sourcePath);
                    source = new String(content, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOG.warn("Unable to read the template from path: " + sourcePath, e);
                }
            }
            source = source.replace("\n", "\\n");
            variantsMap.put(variant, source);
        }
        return variantsMap;
    }

    /**
     * translates Json types to JDK types
     *
     * @param testData
     * @return
     */
    private Object translate(Object testData) {
        if (testData instanceof JsonArray) {
            return translate((JsonArray) testData);
        } else if (testData instanceof JsonObject) {
            return translate((JsonObject) testData);
        }
        return testData;
    }

    private Object translate(JsonArray testData) {
        List<Object> ret = new ArrayList<>();
        for (Object i : testData.getList()) {
            ret.add(translate(i));
        }
        return ret;
    }

    private Object translate(JsonObject testData) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> data = testData.getMap();
        for (String i : testData.fieldNames()) {
            map.put(i, translate(data.get(i)));
        }
        return map;
    }

}

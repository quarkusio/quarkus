package io.quarkus.vertx.http.deployment.devmode.console;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.yaml.snakeyaml.Yaml;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.builder.Version;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.classloader.ClassPathUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * This is a Handler running in the Dev Vert.x instance (which is loaded by the Augmentation ClassLoader)
 * and has access to build time stuff
 */
public class DevConsole implements Handler<RoutingContext> {

    static final ThreadLocal<String> currentExtension = new ThreadLocal<>();
    private static final Comparator<Map<String, Object>> EXTENSION_COMPARATOR = Comparator
            .comparing(m -> ((String) m.get("name")));

    final Engine engine;
    final Map<String, Map<String, Object>> extensions = new HashMap<>();

    final Map<String, Object> globalData = new HashMap<>();

    final Config config = ConfigProvider.getConfig();
    final String devRootAppend;

    DevConsole(Engine engine, String httpRootPath, String frameworkRootPath) {
        this.engine = engine;
        // Both of these paths will end in slash
        this.globalData.put("httpRootPath", httpRootPath);
        this.globalData.put("frameworkRootPath", frameworkRootPath);

        // This includes the dev segment, but does not include a trailing slash (for append)
        this.devRootAppend = frameworkRootPath + "dev";
        this.globalData.put("devRootAppend", devRootAppend);

        this.globalData.put("quarkusVersion", Version.getVersion());
        this.globalData.put("applicationName", config.getOptionalValue("quarkus.application.name", String.class).orElse(""));
        this.globalData.put("applicationVersion",
                config.getOptionalValue("quarkus.application.version", String.class).orElse(""));

        try {
            final Yaml yaml = new Yaml();
            ClassPathUtils.consumeAsPaths("/META-INF/quarkus-extension.yaml", p -> {
                final String desc;
                try (Scanner scanner = new Scanner(Files.newBufferedReader(p, StandardCharsets.UTF_8))) {
                    scanner.useDelimiter("\\A");
                    desc = scanner.hasNext() ? scanner.next() : null;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read " + p, e);
                }
                if (desc == null) {
                    // should be an exception?
                    return;
                }
                final Map<String, Object> metadata = yaml.load(desc);
                extensions.put(getExtensionNamespace(metadata), metadata);
            });
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
        this.globalData.put("configKeyMap", getConfigKeyMap());
    }

    @Override
    public void handle(RoutingContext ctx) {
        // Redirect /q/dev to /q/dev/
        if (ctx.normalizedPath().length() == devRootAppend.length()) {
            ctx.response().setStatusCode(302);
            ctx.response().headers().set(HttpHeaders.LOCATION, devRootAppend + "/");
            ctx.response().end();
            return;
        }

        String path = ctx.normalizedPath().substring(ctx.mountPoint().length() + 1);
        if (path.isEmpty() || path.equals("/")) {
            sendMainPage(ctx);
        } else {
            int nsIndex = path.indexOf("/");
            if (nsIndex == -1) {
                ctx.response().setStatusCode(404).end();
                return;
            }
            String namespace = path.substring(0, nsIndex);
            currentExtension.set(namespace);
            Template devTemplate = engine.getTemplate(path);
            if (devTemplate != null) {
                String extName = getExtensionName(namespace);
                ctx.response().setStatusCode(200).headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                TemplateInstance devTemplateInstance = devTemplate
                        .data("currentExtensionName", extName)
                        .data("flash", FlashScopeUtil.getFlash(ctx))
                        .data("currentRequest", ctx.request());
                renderTemplate(ctx, devTemplateInstance);
            } else {
                ctx.next();
            }
        }
    }

    private Map<String, List<String>> getConfigKeyMap() {
        Map<String, List<String>> ckm = new TreeMap<>();
        Collection<Map<String, Object>> values = this.extensions.values();
        for (Map<String, Object> extension : values) {
            if (extension.containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) extension.get("metadata");
                if (metadata.containsKey("config")) {
                    List<String> configKeys = (List<String>) metadata.get("config");
                    String name = (String) extension.get("name");
                    ckm.put(name, configKeys);
                }
            }
        }
        return ckm;
    }

    private String getExtensionName(String namespace) {
        Map<String, Object> map = extensions.get(namespace);
        if (map == null)
            return null;
        return (String) map.get("name");
    }

    protected void renderTemplate(RoutingContext event, TemplateInstance template) {
        // Add some global variables
        for (Map.Entry<String, Object> global : globalData.entrySet()) {
            template.data(global.getKey(), global.getValue());
        }

        template.renderAsync().handle(new BiFunction<String, Throwable, Object>() {
            @Override
            public Object apply(String s, Throwable throwable) {
                if (throwable != null) {
                    event.fail(throwable);
                } else {
                    event.response().end(s);
                }
                return null;
            }
        });
    }

    public void sendMainPage(RoutingContext event) {
        Template devTemplate = engine.getTemplate("index");
        List<Map<String, Object>> actionableExtensions = new ArrayList<>();
        List<Map<String, Object>> nonActionableExtensions = new ArrayList<>();
        for (Map<String, Object> loaded : this.extensions.values()) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> metadata = (Map<String, Object>) loaded.get("metadata");
            final String namespace = getExtensionNamespace(loaded);
            currentExtension.set(namespace); // needed because the template of the extension is going to be read
            Template simpleTemplate = engine.getTemplate(namespace + "/embedded.html");
            boolean hasConsoleEntry = simpleTemplate != null;
            boolean hasGuide = metadata.containsKey("guide");
            boolean hasConfig = metadata.containsKey("config");
            boolean isUnlisted = metadata.containsKey("unlisted")
                    && (metadata.get("unlisted").equals(true) || metadata.get("unlisted").equals("true"));
            loaded.put("hasConsoleEntry", hasConsoleEntry);
            loaded.put("hasGuide", hasGuide);
            if (!isUnlisted || hasConsoleEntry || hasGuide || hasConfig) {
                if (hasConsoleEntry) {
                    Map<String, Object> data = new HashMap<>();
                    data.putAll(globalData);
                    data.put("urlbase", namespace);
                    String result = simpleTemplate.render(data);
                    loaded.put("_dev", result);
                    actionableExtensions.add(loaded);
                } else {
                    nonActionableExtensions.add(loaded);
                }
            }
        }
        actionableExtensions.sort(EXTENSION_COMPARATOR);
        nonActionableExtensions.sort(EXTENSION_COMPARATOR);
        TemplateInstance instance = devTemplate.data("actionableExtensions", actionableExtensions)
                .data("nonActionableExtensions", nonActionableExtensions).data("flash", FlashScopeUtil.getFlash(event));
        renderTemplate(event, instance);
    }

    private static String getExtensionNamespace(Map<String, Object> metadata) {
        final String groupId;
        final String artifactId;
        final String artifact = (String) metadata.get("artifact");
        if (artifact == null) {
            // trying quarkus 1.x format
            groupId = (String) metadata.get("group-id");
            artifactId = (String) metadata.get("artifact-id");
            if (artifactId == null || groupId == null) {
                throw new RuntimeException(
                        "Failed to locate 'artifact' or 'group-id' and 'artifact-id' among metadata keys " + metadata.keySet());
            }
        } else {
            final AppArtifactCoords coords = AppArtifactCoords.fromString(artifact);
            groupId = coords.getGroupId();
            artifactId = coords.getArtifactId();
        }
        return groupId + "." + artifactId;
    }
}

package io.quarkus.vertx.http.deployment.devmode.console;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.yaml.snakeyaml.Yaml;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.builder.Version;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.vertx.core.Handler;
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

    DevConsole(Engine engine, String httpRootPath, String frameworkRootPath) {
        this.engine = engine;
        this.globalData.put("httpRootPath", httpRootPath);
        this.globalData.put("frameworkRootPath", frameworkRootPath);
        this.globalData.put("quarkusVersion", Version.getVersion());
        this.globalData.put("applicationName", config.getOptionalValue("quarkus.application.name", String.class).orElse(""));
        this.globalData.put("applicationVersion",
                config.getOptionalValue("quarkus.application.version", String.class).orElse(""));

        try {
            Enumeration<URL> extensionDescriptors = getClass().getClassLoader()
                    .getResources("/META-INF/quarkus-extension.yaml");
            Yaml yaml = new Yaml();
            while (extensionDescriptors.hasMoreElements()) {
                URL extensionDescriptor = extensionDescriptors.nextElement();
                String desc = readURL(extensionDescriptor);
                Map<String, Object> loaded = yaml.load(desc);
                String artifactId = (String) loaded.get("artifact-id");
                String groupId = (String) loaded.get("group-id");
                String namespace = groupId + "." + artifactId;
                extensions.put(namespace, loaded);
            }
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void handle(RoutingContext ctx) {
        String path = ctx.normalisedPath().substring(ctx.mountPoint().length());
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
            Map<String, Object> metadata = (Map<String, Object>) loaded.get("metadata");
            String artifactId = (String) loaded.get("artifact-id");
            String groupId = (String) loaded.get("group-id");
            currentExtension.set(groupId + "." + artifactId); // needed because the template of the extension is going to be read
            Template simpleTemplate = engine.getTemplate(groupId + "." + artifactId + "/embedded.html");
            boolean hasConsoleEntry = simpleTemplate != null;
            boolean hasGuide = metadata.containsKey("guide");
            loaded.put("hasConsoleEntry", hasConsoleEntry);
            loaded.put("hasGuide", hasGuide);
            if (hasConsoleEntry || hasGuide) {
                if (hasConsoleEntry) {
                    Map<String, Object> data = new HashMap<>();
                    data.putAll(globalData);
                    data.put("urlbase", groupId + "." + artifactId);
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

    private static String readURL(URL url) throws IOException {
        try (Scanner scanner = new Scanner(url.openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : null;
        }
    }

}

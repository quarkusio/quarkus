package io.quarkus.devui.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.mvnpm.importmap.Aggregator;
import org.mvnpm.importmap.Location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.builder.Version;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.devui.deployment.extension.Extension;
import io.quarkus.devui.deployment.extension.ExtensionGroup;
import io.quarkus.devui.deployment.spi.DevUIContent;
import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.devui.spi.buildtime.QuteTemplateBuildItem;
import io.quarkus.devui.spi.buildtime.StaticContentBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.MenuPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

/**
 * This creates static content that is used in dev UI. For example the index.html and any other data (json) available on build
 * time
 */
public class BuildTimeContentProcessor {
    private static final String SLASH = "/";
    private static final String DEV_UI = "dev-ui";
    private static final String BUILD_TIME_PATH = "dev-ui-templates" + File.separator + "build-time";

    final Config config = ConfigProvider.getConfig();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Here we create references to internal dev ui files so that they can be imported by ref.
     * This will be merged into the final importmap
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    InternalImportMapBuildItem createKnownInternalImportMap(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        String contextRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath() + DEV_UI + SLASH;

        InternalImportMapBuildItem internalImportMapBuildItem = new InternalImportMapBuildItem();

        internalImportMapBuildItem.add("devui/", contextRoot + "/");
        internalImportMapBuildItem.add("qwc/", contextRoot + "qwc/");
        internalImportMapBuildItem.add("qui/", contextRoot + "qui/");
        internalImportMapBuildItem.add("qui-badge", contextRoot + "qui/qui-badge.js");
        internalImportMapBuildItem.add("icon/", contextRoot + "icon/");
        internalImportMapBuildItem.add("font/", contextRoot + "font/");
        internalImportMapBuildItem.add("controller/", contextRoot + "controller/");
        internalImportMapBuildItem.add("log-controller", contextRoot + "controller/log-controller.js");
        internalImportMapBuildItem.add("router-controller", contextRoot + "controller/router-controller.js");
        internalImportMapBuildItem.add("notifier", contextRoot + "controller/notifier.js");
        internalImportMapBuildItem.add("jsonrpc", contextRoot + "controller/jsonrpc.js");
        internalImportMapBuildItem.add("state/", contextRoot + "state/");
        internalImportMapBuildItem.add("theme-state", contextRoot + "state/theme-state.js");
        internalImportMapBuildItem.add("connection-state", contextRoot + "state/connection-state.js");
        internalImportMapBuildItem.add("devui-state", contextRoot + "state/devui-state.js");

        return internalImportMapBuildItem;
    }

    /**
     * Here we map all the pages (as defined by the extensions) build time data
     *
     * @param pageBuildItems
     * @param buildTimeConstProducer
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void mapPageBuildTimeData(List<CardPageBuildItem> pageBuildItems,
            BuildProducer<BuildTimeConstBuildItem> buildTimeConstProducer) {

        for (CardPageBuildItem pageBuildItem : pageBuildItems) {
            Map<String, Object> buildTimeData = getBuildTimeData(pageBuildItem);
            if (!buildTimeData.isEmpty()) {
                buildTimeConstProducer.produce(
                        new BuildTimeConstBuildItem(pageBuildItem.getExtensionName(), buildTimeData));
            }
        }
    }

    private Map<String, Object> getBuildTimeData(CardPageBuildItem pageBuildItem) {
        Map<String, Object> m = new HashMap<>();
        if (pageBuildItem.hasBuildTimeData()) {
            m.putAll(pageBuildItem.getBuildTimeData());
        }

        if (pageBuildItem.getOptionalCard().isPresent()) {
            // Make the pages available for the custom card
            List<Page> pages = new ArrayList<>();
            List<PageBuilder> pageBuilders = pageBuildItem.getPages();
            for (PageBuilder pageBuilder : pageBuilders) {
                pageBuilder.extension(pageBuildItem.getExtensionName());
                pageBuilder.namespace(pageBuildItem.getExtensionName());
                pages.add(pageBuilder.build());
            }

            m.put("pages", pages);
        }
        return m;
    }

    /**
     * Here we find all build time data and make then available via a const
     *
     * js components can import the const with "import {constName} from '{ext}-data';"
     *
     * @param pageBuildItems
     * @param quteTemplateProducer
     * @param internalImportMapProducer
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void createBuildTimeConstJsTemplate(
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            List<BuildTimeConstBuildItem> buildTimeConstBuildItems,
            BuildProducer<QuteTemplateBuildItem> quteTemplateProducer,
            BuildProducer<InternalImportMapBuildItem> internalImportMapProducer) {

        String contextRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath() + DEV_UI + SLASH;

        QuteTemplateBuildItem quteTemplateBuildItem = new QuteTemplateBuildItem(
                QuteTemplateBuildItem.DEV_UI);

        InternalImportMapBuildItem internalImportMapBuildItem = new InternalImportMapBuildItem();

        for (BuildTimeConstBuildItem buildTimeConstBuildItem : buildTimeConstBuildItems) {
            Map<String, Object> data = new HashMap<>();
            if (buildTimeConstBuildItem.hasBuildTimeData()) {
                for (Map.Entry<String, Object> pageData : buildTimeConstBuildItem.getBuildTimeData().entrySet()) {
                    try {
                        String key = pageData.getKey();
                        String value = objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(pageData.getValue());
                        data.put(key, value);
                    } catch (JsonProcessingException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            if (!data.isEmpty()) {
                Map<String, Object> qutedata = new HashMap<>();
                qutedata.put("buildTimeData", data);
                String ref = buildTimeConstBuildItem.getExtensionPathName() + "-data";
                String file = ref + ".js";
                quteTemplateBuildItem.add("build-time-data.js", file, qutedata);
                internalImportMapBuildItem.add(ref, contextRoot + file);
            }
        }

        quteTemplateProducer.produce(quteTemplateBuildItem);
        internalImportMapProducer.produce(internalImportMapBuildItem);
    }

    /**
     * Here we find all the mvnpm jars
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void gatherMvnpmJars(BuildProducer<MvnpmBuildItem> mvnpmProducer, CurateOutcomeBuildItem curateOutcomeBuildItem) {
        Set<URL> mvnpmJars = new HashSet<>();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> jarsWithImportMaps = tccl.getResources(Location.IMPORTMAP_PATH);
            Set<URL> jarUrls = new HashSet<URL>(Collections.list(jarsWithImportMaps));
            for (URL jarUrl : jarUrls) {
                final JarURLConnection connection = (JarURLConnection) jarUrl.openConnection();
                mvnpmJars.add(connection.getJarFileURL());
            }
            mvnpmProducer.produce(new MvnpmBuildItem(mvnpmJars));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Here we create index.html
     * We aggregate all import maps into one
     * This includes import maps from 3rd party libs from mvnpm.org and internal ones defined above
     *
     * @return The QuteTemplate Build item that will create the end result
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    QuteTemplateBuildItem createIndexHtmlTemplate(
            MvnpmBuildItem mvnpmBuildItem,
            ThemeVarsBuildItem themeVarsBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            List<InternalImportMapBuildItem> internalImportMapBuildItems) {
        QuteTemplateBuildItem quteTemplateBuildItem = new QuteTemplateBuildItem(
                QuteTemplateBuildItem.DEV_UI);

        Aggregator aggregator = new Aggregator(mvnpmBuildItem.getMvnpmJars());

        for (InternalImportMapBuildItem importMapBuildItem : internalImportMapBuildItems) {
            Map<String, String> importMap = importMapBuildItem.getImportMap();
            aggregator.addMappings(importMap);
        }
        String importmap = aggregator.aggregateAsJson();
        aggregator.reset();

        String themeVars = themeVarsBuildItem.getTemplateValue();
        String contextRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath() + DEV_UI + SLASH;

        // TODO: Move version and name to build time data

        Map<String, Object> data = Map.of(
                "contextRoot", contextRoot,
                "importmap", importmap,
                "themeVars", themeVars);

        quteTemplateBuildItem.add("index.html", data);

        return quteTemplateBuildItem;
    }

    // Here load all templates
    @BuildStep(onlyIf = IsDevelopment.class)
    void loadAllBuildTimeTemplates(BuildProducer<StaticContentBuildItem> buildTimeContentProducer,
            List<QuteTemplateBuildItem> templates) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (QuteTemplateBuildItem template : templates) {

            List<DevUIContent> contentPerExtension = new ArrayList<>();

            if (template.isInternal()) {
                List<QuteTemplateBuildItem.TemplateData> templatesWithData = template.getTemplateDatas();
                for (QuteTemplateBuildItem.TemplateData e : templatesWithData) {

                    String templateName = e.getTemplateName(); // Relative to BUILD_TIME_PATH
                    Map<String, Object> data = e.getData();
                    String resourceName = BUILD_TIME_PATH + File.separator + templateName;
                    String fileName = e.getFileName();
                    // TODO: What if we find more than one ?
                    try (InputStream templateStream = cl.getResourceAsStream(resourceName)) {
                        if (templateStream != null) {
                            byte[] templateContent = IoUtil.readBytes(templateStream);
                            // Internal runs on "naked" namespace
                            DevUIContent content = DevUIContent.builder()
                                    .fileName(fileName)
                                    .template(templateContent)
                                    .addData(data)
                                    .build();
                            contentPerExtension.add(content);
                        }
                    } catch (IOException ioe) {
                        throw new UncheckedIOException("An error occurred while processing " + resourceName, ioe);
                    }
                }
                buildTimeContentProducer.produce(new StaticContentBuildItem(
                        StaticContentBuildItem.DEV_UI, contentPerExtension));
            }
        }
    }

    /**
     * Creates json data that is available in Javascript
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void createBuildTimeData(BuildProducer<BuildTimeConstBuildItem> buildTimeConstProducer,
            BuildProducer<ThemeVarsBuildItem> themeVarsProducer,
            ExtensionsBuildItem extensionsBuildItem,
            List<MenuPageBuildItem> menuPageBuildItems) {

        BuildTimeConstBuildItem internalBuildTimeData = new BuildTimeConstBuildItem(AbstractDevUIBuildItem.DEV_UI);

        // Theme details TODO: Allow configuration
        Map<String, Map<String, String>> themes = new HashMap<>();
        Map<String, String> dark = new HashMap<>();
        Map<String, String> light = new HashMap<>();

        // Quarkus logo colors
        light.put("--quarkus-blue", QUARKUS_BLUE.toString());
        dark.put("--quarkus-blue", QUARKUS_BLUE.toString());

        light.put("--quarkus-red", QUARKUS_RED.toString());
        dark.put("--quarkus-red", QUARKUS_RED.toString());

        light.put("--quarkus-center", QUARKUS_DARK.toString());
        dark.put("--quarkus-center", QUARKUS_LIGHT.toString());

        // Vaadin's Lumo (see https://vaadin.com/docs/latest/styling/lumo/design-tokens/color)

        // Base
        light.put("--lumo-base-color", Color.from(0, 100, 100).toString());
        dark.put("--lumo-base-color", Color.from(210, 10, 23).toString());

        // Grayscale
        light.put("--lumo-contrast-5pct", Color.from(214, 61, 25, 0.05).toString());
        dark.put("--lumo-contrast-5pct", Color.from(214, 65, 85, 0.06).toString());
        light.put("--lumo-contrast-10pct", Color.from(214, 57, 24, 0.1).toString());
        dark.put("--lumo-contrast-10pct", Color.from(214, 60, 80, 0.14).toString());
        light.put("--lumo-contrast-20pct", Color.from(214, 53, 23, 0.16).toString());
        dark.put("--lumo-contrast-20pct", Color.from(214, 64, 82, 0.23).toString());
        light.put("--lumo-contrast-30pct", Color.from(214, 50, 22, 0.26).toString());
        dark.put("--lumo-contrast-30pct", Color.from(214, 69, 84, 0.32).toString());
        light.put("--lumo-contrast-40pct", Color.from(214, 47, 21, 0.38).toString());
        dark.put("--lumo-contrast-40pct", Color.from(214, 73, 86, 0.41).toString());
        light.put("--lumo-contrast-50pct", Color.from(214, 45, 20, 0.52).toString());
        dark.put("--lumo-contrast-50pct", Color.from(214, 78, 88, 0.50).toString());
        light.put("--lumo-contrast-60pct", Color.from(214, 43, 19, 0.6).toString());
        dark.put("--lumo-contrast-60pct", Color.from(214, 82, 90, 0.6).toString());
        light.put("--lumo-contrast-70pct", Color.from(214, 42, 18, 0.69).toString());
        dark.put("--lumo-contrast-70pct", Color.from(214, 87, 92, 0.7).toString());
        light.put("--lumo-contrast-80pct", Color.from(214, 41, 17, 0.83).toString());
        dark.put("--lumo-contrast-80pct", Color.from(214, 91, 94, 0.8).toString());
        light.put("--lumo-contrast-90pct", Color.from(214, 40, 16, 0.94).toString());
        dark.put("--lumo-contrast-90pct", Color.from(214, 96, 96, 0.9).toString());
        light.put("--lumo-contrast", Color.from(214, 35, 15).toString());
        dark.put("--lumo-contrast", Color.from(214, 100, 98).toString());

        // Primary
        light.put("--lumo-primary-color-10pct", Color.from(214, 100, 60, 0.13).toString());
        dark.put("--lumo-primary-color-10pct", Color.from(214, 90, 63, 0.1).toString());
        light.put("--lumo-primary-color-50pct", Color.from(QUARKUS_BLUE, 0.76).toString());
        dark.put("--lumo-primary-color-50pct", Color.from(QUARKUS_BLUE, 0.5).toString());
        light.put("--lumo-primary-color", QUARKUS_BLUE.toString());
        dark.put("--lumo-primary-color", QUARKUS_BLUE.toString());
        light.put("--lumo-primary-text-color", QUARKUS_BLUE.toString());
        dark.put("--lumo-primary-text-color", QUARKUS_BLUE.toString());
        light.put("--lumo-primary-contrast-color", Color.from(0, 100, 100).toString());
        dark.put("--lumo-primary-contrast-color", Color.from(0, 100, 100).toString());

        // Error
        light.put("--lumo-error-color-10pct", Color.from(3, 85, 49, 0.1).toString());
        dark.put("--lumo-error-color-10pct", Color.from(3, 90, 63, 0.1).toString());
        light.put("--lumo-error-color-50pct", Color.from(3, 85, 49, 0.5).toString());
        dark.put("--lumo-error-color-50pct", Color.from(3, 90, 63, 0.5).toString());
        light.put("--lumo-error-color", Color.from(3, 85, 48).toString());
        dark.put("--lumo-error-color", Color.from(3, 90, 63).toString());
        light.put("--lumo-error-text-color", Color.from(3, 89, 42).toString());
        dark.put("--lumo-error-text-color", Color.from(3, 100, 67).toString());
        light.put("--lumo-error-contrast-color", Color.from(0, 100, 100).toString());
        dark.put("--lumo-error-contrast-color", Color.from(0, 100, 100).toString());

        // Success
        light.put("--lumo-success-color-10pct", Color.from(145, 72, 31, 0.1).toString());
        dark.put("--lumo-success-color-10pct", Color.from(145, 65, 42, 0.1).toString());
        light.put("--lumo-success-color-50pct", Color.from(145, 72, 31, 0.5).toString());
        dark.put("--lumo-success-color-50pct", Color.from(145, 65, 42, 0.5).toString());
        light.put("--lumo-success-color", Color.from(145, 72, 30).toString());
        dark.put("--lumo-success-color", Color.from(145, 65, 42).toString());
        light.put("--lumo-success-text-color", Color.from(145, 85, 25).toString());
        dark.put("--lumo-success-text-color", Color.from(145, 85, 47).toString());
        light.put("--lumo-success-contrast-color", Color.from(0, 100, 100).toString());
        dark.put("--lumo-success-contrast-color", Color.from(0, 100, 100).toString());

        // Text
        light.put("--lumo-header-text-color", Color.from(214, 35, 15).toString());
        dark.put("--lumo-header-text-color", Color.from(214, 100, 98).toString());
        light.put("--lumo-body-text-color", Color.from(214, 40, 16, 0.94).toString());
        dark.put("--lumo-body-text-color", Color.from(214, 96, 96, 0.9).toString());
        light.put("--lumo-secondary-text-color", Color.from(214, 42, 18, 0.69).toString());
        dark.put("--lumo-secondary-text-color", Color.from(214, 87, 92, 0.7).toString());
        light.put("--lumo-tertiary-text-color", Color.from(214, 45, 20, 0.52).toString());
        dark.put("--lumo-tertiary-text-color", Color.from(214, 78, 88, 0.5).toString());
        light.put("--lumo-disabled-text-color", Color.from(214, 50, 22, 0.26).toString());
        dark.put("--lumo-disabled-text-color", Color.from(214, 69, 84, 0.32).toString());

        themes.put("dark", dark);
        themes.put("light", light);

        internalBuildTimeData.addBuildTimeData("themes", themes);

        // Extensions
        Map<ExtensionGroup, List<Extension>> response = Map.of(
                ExtensionGroup.active, extensionsBuildItem.getActiveExtensions(),
                ExtensionGroup.inactive, extensionsBuildItem.getInactiveExtensions());

        internalBuildTimeData.addBuildTimeData("extensions", response);

        // Sections Menu
        Page extensions = Page.webComponentPageBuilder().internal()
                .title("Extensions")
                .icon("font-awesome-solid:puzzle-piece")
                .componentLink("qwc-extensions.js").build();

        Page configuration = Page.webComponentPageBuilder().internal()
                .title("Configuration")
                .icon("font-awesome-solid:sliders")
                .componentLink("qwc-configuration.js").build();

        internalBuildTimeData.addBuildTimeData("allConfiguration", "TODO: Configuration");

        Page continuousTesting = Page.webComponentPageBuilder().internal()
                .title("Continuous Testing")
                .icon("font-awesome-solid:flask-vial")
                .componentLink("qwc-continuous-testing.js").build();

        internalBuildTimeData.addBuildTimeData("continuousTesting", "TODO: Continuous Testing");

        Page devServices = Page.webComponentPageBuilder().internal()
                .title("Dev services")
                .icon("font-awesome-solid:wand-magic-sparkles")
                .componentLink("qwc-dev-services.js").build();

        internalBuildTimeData.addBuildTimeData("devServices", "TODO: Dev Services");

        Page buildSteps = Page.webComponentPageBuilder().internal()
                .title("Build steps")
                .icon("font-awesome-solid:hammer")
                .componentLink("qwc-build-steps.js").build();

        internalBuildTimeData.addBuildTimeData("buildSteps", "TODO: Build Steps");

        // Add default menu items
        @SuppressWarnings("unchecked")
        List<Page> sectionMenu = new ArrayList(List.of(extensions, configuration, continuousTesting, devServices, buildSteps));

        // Add any Menus from extensions
        for (Extension e : extensionsBuildItem.getSectionMenuExtensions()) {
            List<Page> pagesFromExtension = e.getMenuPages();
            sectionMenu.addAll(pagesFromExtension);
        }

        internalBuildTimeData.addBuildTimeData("menuItems", sectionMenu);

        // Add the Footer tabs
        Page serverLog = Page.webComponentPageBuilder().internal()
                .title("Server")
                .icon("font-awesome-solid:server")
                .componentLink("qwc-server-log.js").build();

        Page devUiLog = Page.webComponentPageBuilder().internal()
                .title("Dev UI")
                .icon("font-awesome-solid:satellite-dish")
                .componentLink("qwc-jsonrpc-messages.js").build();

        @SuppressWarnings("unchecked")
        List<Page> footerTabs = new ArrayList(List.of(serverLog, devUiLog));

        // Add any Footer tabs from extensions
        for (Extension e : extensionsBuildItem.getFooterTabsExtensions()) {
            List<Page> tabsFromExtension = e.getFooterPages();
            footerTabs.addAll(tabsFromExtension);
        }

        internalBuildTimeData.addBuildTimeData("footerTabs", footerTabs);

        // Add version info
        Map<String, String> applicationInfo = new HashMap<>();
        applicationInfo.put("quarkusVersion", Version.getVersion());
        applicationInfo.put("applicationName", config.getOptionalValue("quarkus.application.name", String.class).orElse(""));
        applicationInfo.put("applicationVersion",
                config.getOptionalValue("quarkus.application.version", String.class).orElse(""));
        internalBuildTimeData.addBuildTimeData("applicationInfo", applicationInfo);

        buildTimeConstProducer.produce(internalBuildTimeData);

        themeVarsProducer.produce(new ThemeVarsBuildItem(light.keySet(), QUARKUS_BLUE.toString()));
    }

    private static final Color QUARKUS_BLUE = Color.from(211, 63, 54);
    private static final Color QUARKUS_RED = Color.from(343, 100, 50);
    private static final Color QUARKUS_DARK = Color.from(180, 36, 5);
    private static final Color QUARKUS_LIGHT = Color.from(0, 0, 90);

    /**
     * This represents a HSLA color
     * see https://www.w3schools.com/html/html_colors_hsl.asp
     */
    static class Color {
        private int hue; // Defines a degree on the color wheel (from 0 to 360) - 0 (or 360) is red, 120 is green, 240 is blue
        private int saturation; // Defines the saturation; 0% is a shade of gray and 100% is the full color (full saturation)
        private int lightness; // Defines the lightness; 0% is black, 50% is normal, and 100% is white
        private double alpha; // Defines the opacity; 0 is fully transparent, 100 is not transparent at all

        private Color(int hue, int saturation, int lightness, double alpha) {
            if (hue < 0 || hue > 360) {
                throw new RuntimeException(
                        "Invalid hue, number needs to be between 0 and 360. Defines a degree on the color wheel");
            }
            this.hue = hue;

            if (saturation < 0 || saturation > 100) {
                throw new RuntimeException(
                        "Invalid saturation, number needs to be between 0 and 100. 0% is a shade of gray and 100% is the full color (full saturation)");
            }
            this.saturation = saturation;

            if (lightness < 0 || lightness > 100) {
                throw new RuntimeException(
                        "Invalid lightness, number needs to be between 0 and 100. 0% is black, 50% is normal, and 100% is white");
            }
            this.lightness = lightness;

            if (alpha < 0 || alpha > 1) {
                throw new RuntimeException(
                        "Invalid alpha, number needs to be between 0 and 1. 0 is fully transparent, 1 is not transparent at all");
            }
            this.alpha = alpha;
        }

        @Override
        public String toString() {
            return "hsla(" + this.hue + ", " + this.saturation + "%, " + this.lightness + "%, " + this.alpha + ")";
        }

        static Color from(Color color, double alpha) {
            return new Color(color.hue, color.saturation, color.lightness, alpha);
        }

        static Color from(int hue, int saturation, int lightness) {
            return new Color(hue, saturation, lightness, 1);
        }

        static Color from(int hue, int saturation, int lightness, double alpha) {
            return new Color(hue, saturation, lightness, alpha);
        }
    }
}

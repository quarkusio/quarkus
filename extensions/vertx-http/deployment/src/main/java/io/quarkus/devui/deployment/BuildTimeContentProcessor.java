package io.quarkus.devui.deployment;

import static java.util.logging.Level.ALL;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.OFF;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.jboss.logmanager.Level.DEBUG;
import static org.jboss.logmanager.Level.ERROR;
import static org.jboss.logmanager.Level.FATAL;
import static org.jboss.logmanager.Level.INFO;
import static org.jboss.logmanager.Level.TRACE;
import static org.jboss.logmanager.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.mvnpm.importmap.Aggregator;
import io.mvnpm.importmap.Location;
import io.quarkus.builder.Version;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.ide.EffectiveIdeBuildItem;
import io.quarkus.deployment.ide.Ide;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.deployment.extension.Extension;
import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.devui.spi.DevUIContent;
import io.quarkus.devui.spi.buildtime.QuteTemplateBuildItem;
import io.quarkus.devui.spi.buildtime.StaticContentBuildItem;
import io.quarkus.devui.spi.page.AbstractPageBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.MenuPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.vertx.core.json.jackson.DatabindCodec;

/**
 * This creates static content that is used in dev UI. For example the index.html and any other data (json) available on build
 * time
 */
public class BuildTimeContentProcessor {
    private static final Logger log = Logger.getLogger(BuildTimeContentProcessor.class);

    private static final String SLASH = "/";
    private static final String DEV_UI = "dev-ui";
    private static final String BUILD_TIME_PATH = "dev-ui-templates/build-time";
    private static final String ES_MODULE_SHIMS = "es-module-shims";

    final Config config = ConfigProvider.getConfig();

    /**
     * Here we create references to internal dev ui files so that they can be imported by ref.
     * This will be merged into the final importmap
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    InternalImportMapBuildItem createKnownInternalImportMap(NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        String contextRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath() + DEV_UI + SLASH;

        InternalImportMapBuildItem internalImportMapBuildItem = new InternalImportMapBuildItem();

        internalImportMapBuildItem.add("devui/", contextRoot);
        // Quarkus Web Components
        internalImportMapBuildItem.add("qwc/", contextRoot + "qwc/");
        internalImportMapBuildItem.add("qwc-no-data", contextRoot + "qwc/qwc-no-data.js");
        internalImportMapBuildItem.add("qwc-hot-reload-element", contextRoot + "qwc/qwc-hot-reload-element.js");
        internalImportMapBuildItem.add("qwc-server-log", contextRoot + "qwc/qwc-server-log.js");
        internalImportMapBuildItem.add("qwc-extension-link", contextRoot + "qwc/qwc-extension-link.js");
        // Quarkus UI
        internalImportMapBuildItem.add("qui/", contextRoot + "qui/");
        internalImportMapBuildItem.add("qui-card", contextRoot + "qui/qui-card.js");

        internalImportMapBuildItem.add("qui-badge", contextRoot + "qui/qui-badge.js");
        internalImportMapBuildItem.add("qui-alert", contextRoot + "qui/qui-alert.js");
        internalImportMapBuildItem.add("qui-code-block", contextRoot + "qui/qui-code-block.js");
        internalImportMapBuildItem.add("qui-ide-link", contextRoot + "qui/qui-ide-link.js");

        // Echarts
        internalImportMapBuildItem.add("echarts/", contextRoot + "echarts/");
        internalImportMapBuildItem.add("echarts-gauge-grade", contextRoot + "echarts/echarts-gauge-grade.js");
        internalImportMapBuildItem.add("echarts-pie", contextRoot + "echarts/echarts-pie.js");
        internalImportMapBuildItem.add("echarts-horizontal-stacked-bar",
                contextRoot + "echarts/echarts-horizontal-stacked-bar.js");
        internalImportMapBuildItem.add("echarts-force-graph",
                contextRoot + "echarts/echarts-force-graph.js");
        internalImportMapBuildItem.add("echarts-bar-stack",
                contextRoot + "echarts/echarts-bar-stack.js");

        // Other assets
        internalImportMapBuildItem.add("icon/", contextRoot + "icon/");
        // Controllers
        internalImportMapBuildItem.add("controller/", contextRoot + "controller/");
        internalImportMapBuildItem.add("log-controller", contextRoot + "controller/log-controller.js");
        internalImportMapBuildItem.add("storage-controller", contextRoot + "controller/storage-controller.js");
        internalImportMapBuildItem.add("router-controller", contextRoot + "controller/router-controller.js");
        internalImportMapBuildItem.add("notifier", contextRoot + "controller/notifier.js");
        internalImportMapBuildItem.add("jsonrpc", contextRoot + "controller/jsonrpc.js");
        // State
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
    void mapPageBuildTimeData(List<CardPageBuildItem> cards,
            List<MenuPageBuildItem> menus,
            List<FooterPageBuildItem> footers,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<BuildTimeConstBuildItem> buildTimeConstProducer) {

        for (CardPageBuildItem card : cards) {
            String extensionPathName = card.getExtensionPathName(curateOutcomeBuildItem);
            Map<String, Object> buildTimeData = getBuildTimeDataForCard(curateOutcomeBuildItem, card);
            if (!buildTimeData.isEmpty()) {
                buildTimeConstProducer.produce(
                        new BuildTimeConstBuildItem(extensionPathName, buildTimeData));
            }
        }
        for (MenuPageBuildItem menu : menus) {
            String extensionPathName = menu.getExtensionPathName(curateOutcomeBuildItem);
            Map<String, Object> buildTimeData = getBuildTimeDataForPage(menu);
            if (!buildTimeData.isEmpty()) {
                buildTimeConstProducer.produce(
                        new BuildTimeConstBuildItem(extensionPathName, buildTimeData));
            }
        }
        for (FooterPageBuildItem footer : footers) {
            String extensionPathName = footer.getExtensionPathName(curateOutcomeBuildItem);
            Map<String, Object> buildTimeData = getBuildTimeDataForPage(footer);
            if (!buildTimeData.isEmpty()) {
                buildTimeConstProducer.produce(
                        new BuildTimeConstBuildItem(extensionPathName, buildTimeData));
            }
        }
    }

    private Map<String, Object> getBuildTimeDataForPage(AbstractPageBuildItem pageBuildItem) {
        Map<String, Object> m = new HashMap<>();
        if (pageBuildItem.hasBuildTimeData()) {
            m.putAll(pageBuildItem.getBuildTimeData());
        }
        return m;
    }

    private Map<String, Object> getBuildTimeDataForCard(CurateOutcomeBuildItem curateOutcomeBuildItem,
            CardPageBuildItem pageBuildItem) {
        Map<String, Object> m = getBuildTimeDataForPage(pageBuildItem);

        if (pageBuildItem.getOptionalCard().isPresent()) {
            // Make the pages available for the custom card
            List<Page> pages = new ArrayList<>();
            List<PageBuilder> pageBuilders = pageBuildItem.getPages();
            for (PageBuilder pageBuilder : pageBuilders) {
                String path = pageBuildItem.getExtensionPathName(curateOutcomeBuildItem);
                pageBuilder.namespace(path);
                pageBuilder.extension(path);
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
    void createBuildTimeConstJsTemplate(CurateOutcomeBuildItem curateOutcomeBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            List<BuildTimeConstBuildItem> buildTimeConstBuildItems,
            BuildProducer<QuteTemplateBuildItem> quteTemplateProducer,
            BuildProducer<InternalImportMapBuildItem> internalImportMapProducer) {

        String contextRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath() + DEV_UI + SLASH;

        QuteTemplateBuildItem quteTemplateBuildItem = new QuteTemplateBuildItem(
                QuteTemplateBuildItem.DEV_UI);

        InternalImportMapBuildItem internalImportMapBuildItem = new InternalImportMapBuildItem();

        var mapper = DatabindCodec.mapper().writerWithDefaultPrettyPrinter();
        for (BuildTimeConstBuildItem buildTimeConstBuildItem : buildTimeConstBuildItems) {
            Map<String, Object> data = new HashMap<>();
            if (buildTimeConstBuildItem.hasBuildTimeData()) {
                for (Map.Entry<String, Object> pageData : buildTimeConstBuildItem.getBuildTimeData().entrySet()) {
                    try {
                        String key = pageData.getKey();
                        String value = mapper.writeValueAsString(pageData.getValue());
                        data.put(key, value);
                    } catch (JsonProcessingException ex) {
                        log.error("Could not create Json Data for Dev UI page", ex);
                    }
                }
            }
            if (!data.isEmpty()) {
                Map<String, Object> qutedata = new HashMap<>();
                qutedata.put("buildTimeData", data);

                String ref = buildTimeConstBuildItem.getExtensionPathName(curateOutcomeBuildItem) + "-data";
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
            while (jarsWithImportMaps.hasMoreElements()) {
                URL jarUrl = jarsWithImportMaps.nextElement();
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
        String esModuleShimsVersion = extractEsModuleShimsVersion(mvnpmBuildItem.getMvnpmJars());
        String importmap = aggregator.aggregateAsJson(nonApplicationRootPathBuildItem.getNonApplicationRootPath());
        aggregator.reset();

        String themeVars = themeVarsBuildItem.getTemplateValue();
        String nonApplicationRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath();
        String contextRoot = nonApplicationRoot + DEV_UI + SLASH;

        Map<String, Object> data = Map.of(
                "nonApplicationRoot", nonApplicationRoot,
                "contextRoot", contextRoot,
                "importmap", importmap,
                "themeVars", themeVars,
                "esModuleShimsVersion", esModuleShimsVersion);

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

            List<QuteTemplateBuildItem.TemplateData> templatesWithData = template.getTemplateDatas();
            for (QuteTemplateBuildItem.TemplateData e : templatesWithData) {

                String templateName = e.getTemplateName(); // Relative to BUILD_TIME_PATH
                Map<String, Object> data = e.getData();
                String resourceName = BUILD_TIME_PATH + SLASH + templateName;
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

    /**
     * Creates json data that is available in Javascript
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    void createBuildTimeData(BuildProducer<BuildTimeConstBuildItem> buildTimeConstProducer,
            BuildProducer<ThemeVarsBuildItem> themeVarsProducer,
            List<InternalPageBuildItem> internalPages,
            ExtensionsBuildItem extensionsBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem) {

        BuildTimeConstBuildItem internalBuildTimeData = new BuildTimeConstBuildItem(AbstractDevUIBuildItem.DEV_UI);

        addThemeBuildTimeData(internalBuildTimeData, themeVarsProducer);
        addMenuSectionBuildTimeData(internalBuildTimeData, internalPages, extensionsBuildItem);
        addFooterTabBuildTimeData(internalBuildTimeData, extensionsBuildItem);
        addVersionInfoBuildTimeData(internalBuildTimeData, nonApplicationRootPathBuildItem);
        addIdeBuildTimeData(internalBuildTimeData, effectiveIdeBuildItem, launchModeBuildItem);
        buildTimeConstProducer.produce(internalBuildTimeData);
    }

    private String extractEsModuleShimsVersion(Set<URL> urls) {
        for (URL u : urls) {
            if (u.getPath().contains(ES_MODULE_SHIMS)) {
                int i = u.getPath().indexOf(ES_MODULE_SHIMS) + ES_MODULE_SHIMS.length() + 1;
                String versionOnward = u.getPath().substring(i);
                String[] parts = versionOnward.split(SLASH);
                return parts[0];
            }
        }
        return "";
    }

    private void addThemeBuildTimeData(BuildTimeConstBuildItem internalBuildTimeData,
            BuildProducer<ThemeVarsBuildItem> themeVarsProducer) {
        // Theme details TODO: Allow configuration
        Map<String, Map<String, String>> themes = new HashMap<>();
        Map<String, String> dark = new HashMap<>();
        Map<String, String> light = new HashMap<>();

        computeColors(themes, dark, light);

        internalBuildTimeData.addBuildTimeData("themes", themes);

        // Also set at least one there for a default
        themeVarsProducer.produce(new ThemeVarsBuildItem(light.keySet(), QUARKUS_BLUE.toString()));
    }

    private void addMenuSectionBuildTimeData(BuildTimeConstBuildItem internalBuildTimeData,
            List<InternalPageBuildItem> internalPages,
            ExtensionsBuildItem extensionsBuildItem) {
        // Menu section
        @SuppressWarnings("unchecked")
        List<Page> sectionMenu = new ArrayList();
        Collections.sort(internalPages, (t, t1) -> {
            return ((Integer) t.getPosition()).compareTo(t1.getPosition());
        });

        for (InternalPageBuildItem internalPageBuildItem : internalPages) {
            List<Page> pages = internalPageBuildItem.getPages();
            for (Page page : pages) {
                sectionMenu.add(page);
            }
            internalBuildTimeData.addAllBuildTimeData(internalPageBuildItem.getBuildTimeData());
        }

        // Menus from extensions
        for (Extension e : extensionsBuildItem.getSectionMenuExtensions()) {
            List<Page> pagesFromExtension = e.getMenuPages();
            sectionMenu.addAll(pagesFromExtension);
        }

        internalBuildTimeData.addBuildTimeData("menuItems", sectionMenu);
    }

    private void addFooterTabBuildTimeData(BuildTimeConstBuildItem internalBuildTimeData,
            ExtensionsBuildItem extensionsBuildItem) {
        // Add the Footer tabs
        @SuppressWarnings("unchecked")
        List<Page> footerTabs = new ArrayList();
        Page serverLog = Page.webComponentPageBuilder().internal()
                .namespace("devui-logstream")
                .title("Server")
                .icon("font-awesome-solid:server")
                .componentLink("qwc-server-log.js").build();
        footerTabs.add(serverLog);

        Page testLog = Page.webComponentPageBuilder().internal()
                .namespace("devui-continuous-testing")
                .title("Testing")
                .icon("font-awesome-solid:flask-vial")
                .componentLink("qwc-test-log.js").build();
        footerTabs.add(testLog);

        // This is only needed when extension developers work on an extension, so we only included it if you build from source.
        if (Version.getVersion().equalsIgnoreCase("999-SNAPSHOT")) {
            Page devUiLog = Page.webComponentPageBuilder().internal()
                    .namespace("devui-jsonrpcstream")
                    .title("Dev UI")
                    .icon("font-awesome-solid:satellite-dish")
                    .componentLink("qwc-jsonrpc-messages.js").build();
            footerTabs.add(devUiLog);
        }
        // Add any Footer tabs from extensions
        for (Extension e : extensionsBuildItem.getFooterTabsExtensions()) {
            List<Page> tabsFromExtension = e.getFooterPages();
            footerTabs.addAll(tabsFromExtension);
        }

        internalBuildTimeData.addBuildTimeData("footerTabs", footerTabs);
        internalBuildTimeData.addBuildTimeData("loggerLevels", LEVELS);
    }

    private void addVersionInfoBuildTimeData(BuildTimeConstBuildItem internalBuildTimeData,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        // Add version info
        String contextRoot = nonApplicationRootPathBuildItem.getNonApplicationRootPath() + DEV_UI + SLASH;
        Map<String, String> applicationInfo = new HashMap<>();
        applicationInfo.put("contextRoot", contextRoot);
        applicationInfo.put("quarkusVersion", Version.getVersion());
        applicationInfo.put("applicationName", config.getOptionalValue("quarkus.application.name", String.class).orElse(""));
        applicationInfo.put("applicationVersion",
                config.getOptionalValue("quarkus.application.version", String.class).orElse(""));
        internalBuildTimeData.addBuildTimeData("applicationInfo", applicationInfo);
    }

    private void addIdeBuildTimeData(BuildTimeConstBuildItem internalBuildTimeData,
            Optional<EffectiveIdeBuildItem> effectiveIdeBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {

        Map<String, Object> ideInfo = new HashMap<>();
        boolean disable = launchModeBuildItem.getDevModeType().orElse(DevModeType.LOCAL) != DevModeType.LOCAL;
        ideInfo.put("disable", disable);
        if (effectiveIdeBuildItem.isPresent()) {
            EffectiveIdeBuildItem eibi = effectiveIdeBuildItem.get();
            if (!disable) {
                // Add IDE info
                Ide ide = eibi.getIde();
                ideInfo.put("ideName", ide.name());
                ideInfo.put("idePackages", getAllUserPackages());
            }
        }
        internalBuildTimeData.addBuildTimeData("ideInfo", ideInfo);
    }

    private List<String> getAllUserPackages() {
        List<Path> sourcesDir = DevConsoleManager.getHotReplacementContext().getSourcesDir();
        List<String> packages = new ArrayList<>();

        for (Path sourcePaths : sourcesDir) {
            packages.addAll(sourcePackagesForRoot(sourcePaths));
        }
        return packages;
    }

    /**
     * Return the most general packages used in the application
     * <p>
     * TODO: this likely covers almost all typical use cases, but probably needs some tweaks for extreme corner cases
     */
    private List<String> sourcePackagesForRoot(Path langPath) {
        if (!Files.exists(langPath)) {
            return Collections.emptyList();
        }
        File[] rootFiles = langPath.toFile().listFiles();
        List<Path> rootPackages = new ArrayList<>(1);
        if (rootFiles != null) {
            for (File rootFile : rootFiles) {
                if (rootFile.isDirectory()) {
                    rootPackages.add(rootFile.toPath());
                }
            }
        }
        if (rootPackages.isEmpty()) {
            return List.of("");
        }
        List<String> result = new ArrayList<>(rootPackages.size());
        for (Path rootPackage : rootPackages) {
            List<String> paths = new ArrayList<>();
            SimpleFileVisitor<Path> simpleFileVisitor = new DetectPackageFileVisitor(paths);
            try {
                Files.walkFileTree(rootPackage, simpleFileVisitor);
                if (paths.isEmpty()) {
                    continue;
                }
                String commonPath = commonPath(paths);
                String rootPackageStr = commonPath.replace(langPath.toAbsolutePath().toString(), "")
                        .replace(File.separator, ".");
                if (rootPackageStr.startsWith(".")) {
                    rootPackageStr = rootPackageStr.substring(1);
                }
                if (rootPackageStr.endsWith(".")) {
                    rootPackageStr = rootPackageStr.substring(0, rootPackageStr.length() - 1);
                }
                result.add(rootPackageStr);
            } catch (IOException e) {
                log.debug("Unable to determine the sources directories", e);
                // just ignore it as it's not critical for the DevUI functionality
            }
        }
        return result;
    }

    private String commonPath(List<String> paths) {
        String commonPath = "";
        List<String[]> dirs = new ArrayList<>(paths.size());
        for (int i = 0; i < paths.size(); i++) {
            dirs.add(i, paths.get(i).split(Pattern.quote(File.separator)));
        }
        for (int j = 0; j < dirs.get(0).length; j++) {
            String thisDir = dirs.get(0)[j]; // grab the next directory name in the first path
            boolean allMatched = true;
            for (int i = 1; i < dirs.size() && allMatched; i++) { // look at the other paths
                if (dirs.get(i).length < j) { //there is no directory
                    allMatched = false;
                    break;
                }
                allMatched = dirs.get(i)[j].equals(thisDir); //check if it matched
            }
            if (allMatched) {
                commonPath += thisDir + File.separator;
            } else {
                break;
            }
        }
        return commonPath;
    }

    private static final List<String> LEVELS = List.of(
            OFF.getName(),
            SEVERE.getName(),
            ERROR.getName(),
            FATAL.getName(),
            WARNING.getName(),
            WARN.getName(),
            INFO.getName(),
            DEBUG.getName(),
            TRACE.getName(),
            CONFIG.getName(),
            FINE.getName(),
            FINER.getName(),
            FINEST.getName(),
            ALL.getName());

    private static void computeColors(Map<String, Map<String, String>> themes, Map<String, String> dark,
            Map<String, String> light) {
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

        // Warning
        light.put("--lumo-warning-color-10pct", Color.from(30, 100, 50, 0.1).toString());
        dark.put("--lumo-warning-color-10pct", Color.from(30, 100, 50, 0.1).toString());
        light.put("--lumo-warning-color-50pct", Color.from(30, 100, 50, 0.5).toString());
        dark.put("--lumo-warning-color-50pct", Color.from(30, 100, 50, 0.5).toString());
        light.put("--lumo-warning-color", Color.from(30, 100, 50).toString());
        dark.put("--lumo-warning-color", Color.from(30, 100, 50).toString());
        light.put("--lumo-warning-text-color", Color.from(30, 89, 42).toString());
        dark.put("--lumo-warning-text-color", Color.from(30, 100, 67).toString());
        light.put("--lumo-warning-contrast-color", Color.from(0, 100, 100).toString());
        dark.put("--lumo-warning-contrast-color", Color.from(0, 100, 100).toString());

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

    private static class DetectPackageFileVisitor extends SimpleFileVisitor<Path> {
        private final List<String> paths;

        public DetectPackageFileVisitor(List<String> paths) {
            this.paths = paths;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            boolean hasRegularFiles = false;
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        hasRegularFiles = true;
                        break;
                    }
                }
            }
            if (hasRegularFiles) {
                paths.add(dir.toAbsolutePath().toString());
            }
            return FileVisitResult.CONTINUE;
        }
    }
}

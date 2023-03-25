package io.quarkus.qute.deployment.devui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.qute.ParameterDeclaration;
import io.quarkus.qute.deployment.CheckedTemplateBuildItem;
import io.quarkus.qute.deployment.TemplateDataBuildItem;
import io.quarkus.qute.deployment.TemplateExtensionMethodBuildItem;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.qute.deployment.TemplateVariantsBuildItem;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class QuteDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void pages(
            List<TemplatePathBuildItem> templatePaths,
            List<CheckedTemplateBuildItem> checkedTemplates,
            TemplateVariantsBuildItem variants,
            TemplatesAnalysisBuildItem templatesAnalysis,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TemplateDataBuildItem> templateDatas,
            BuildProducer<CardPageBuildItem> cardPages) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        List<TemplatePathBuildItem> sortedTemplatePaths = templatePaths.stream()
                .sorted(Comparator.comparing(tp -> tp.getPath().toLowerCase())).collect(Collectors.toList());
        JsonArray templates = createTemplatesJson(sortedTemplatePaths, checkedTemplates, templatesAnalysis, variants);

        List<TemplateExtensionMethodBuildItem> sortedExtensionMethods = templateExtensionMethods.stream()
                .sorted(new Comparator<TemplateExtensionMethodBuildItem>() {

                    @Override
                    public int compare(TemplateExtensionMethodBuildItem m1, TemplateExtensionMethodBuildItem m2) {
                        DotName m1Class = m1.getMethod().declaringClass().name();
                        DotName m2Class = m2.getMethod().declaringClass().name();
                        int ret = m1Class.compareTo(m2Class);
                        return ret == 0 ? m1.getMethod().name().compareTo(m2.getMethod().name()) : ret;
                    }
                }).collect(Collectors.toList());
        JsonArray extensionMethods = createExtensionMethodsJson(sortedExtensionMethods);

        List<TemplateDataBuildItem> sortedTemplateData = templateDatas.stream()
                .sorted(Comparator.comparing(td -> td.getTargetClass().name())).collect(Collectors.toList());
        JsonArray templateData = createTemplateDataJson(sortedTemplateData);

        pageBuildItem.addBuildTimeData("templates", templates);
        pageBuildItem.addBuildTimeData("extensionMethods", extensionMethods);
        pageBuildItem.addBuildTimeData("templateData", templateData);

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Templates")
                .icon("font-awesome-solid:file-code")
                .componentLink("qwc-qute-templates.js")
                .staticLabel(String.valueOf(templates.size())));

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Extension Methods")
                .icon("font-awesome-solid:puzzle-piece")
                .componentLink("qwc-qute-extension-methods.js")
                .staticLabel(String.valueOf(extensionMethods.size())));

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("@TemplateData")
                .icon("font-awesome-solid:database")
                .componentLink("qwc-qute-template-data.js")
                .staticLabel(String.valueOf(templateData.size())));

        cardPages.produce(pageBuildItem);
    }

    private JsonArray createTemplateDataJson(List<TemplateDataBuildItem> sortedTemplateData) {
        JsonArray data = new JsonArray();
        for (TemplateDataBuildItem templateData : sortedTemplateData) {
            JsonObject json = new JsonObject();
            json.put("target", templateData.getTargetClass().name().toString());
            if (templateData.hasNamespace()) {
                json.put("namespace", templateData.getNamespace());
            }
            if (templateData.getIgnore() != null && templateData.getIgnore().length > 0) {
                json.put("ignores", Arrays.toString(templateData.getIgnore()));
            }
            if (templateData.isProperties()) {
                json.put("properties", true);
            }
            data.add(json);
        }
        return data;
    }

    private JsonArray createExtensionMethodsJson(List<TemplateExtensionMethodBuildItem> sortedExtensionMethods) {
        JsonArray extensionMethods = new JsonArray();
        for (TemplateExtensionMethodBuildItem templateExtensionMethod : sortedExtensionMethods) {
            JsonObject extensionMethod = new JsonObject();
            extensionMethod.put("name", templateExtensionMethod.getMethod().declaringClass().name() + "#"
                    + templateExtensionMethod.getMethod().name() + "()");
            if (templateExtensionMethod.getMatchRegex() != null && !templateExtensionMethod.getMatchRegex().isEmpty()) {
                extensionMethod.put("matchRegex", templateExtensionMethod.getMatchRegex());
            } else if (!templateExtensionMethod.getMatchNames().isEmpty()) {
                extensionMethod.put("matchNames", templateExtensionMethod.getMatchNames().toString());
            } else {
                extensionMethod.put("matchName", templateExtensionMethod.getMatchName());
            }
            if (templateExtensionMethod.hasNamespace()) {
                extensionMethod.put("namespace", templateExtensionMethod.getNamespace());
            } else {
                extensionMethod.put("matchType", templateExtensionMethod.getMatchType().toString());
            }
            extensionMethods.add(extensionMethod);
        }
        return extensionMethods;
    }

    private JsonArray createTemplatesJson(List<TemplatePathBuildItem> sortedTemplatePaths,
            List<CheckedTemplateBuildItem> checkedTemplates, TemplatesAnalysisBuildItem templatesAnalysis,
            TemplateVariantsBuildItem variants) {
        JsonArray templates = new JsonArray();
        for (TemplatePathBuildItem templatePath : sortedTemplatePaths) {
            JsonObject template = new JsonObject();
            template.put("path", templatePath.getPath());

            CheckedTemplateBuildItem checkedTemplate = findCheckedTemplate(getBasePath(templatePath.getPath(), variants),
                    checkedTemplates);
            if (checkedTemplate != null) {
                template.put("checkedTemplateMethod",
                        checkedTemplate.method.declaringClass().name() + "#" + checkedTemplate.method.name() + "()");
            }

            TemplateAnalysis analysis = templatesAnalysis.getAnalysis().stream()
                    .filter(ta -> ta.path.equals(templatePath.getPath())).findFirst().orElse(null);
            if (analysis != null) {
                if (!analysis.fragmentIds.isEmpty()) {
                    JsonArray fragmentIds = new JsonArray();
                    analysis.fragmentIds.forEach(fragmentIds::add);
                    template.put("fragmentIds", fragmentIds);
                }
                if (!analysis.parameterDeclarations.isEmpty()) {
                    JsonArray paramDeclarations = new JsonArray();
                    for (ParameterDeclaration pd : analysis.parameterDeclarations) {
                        paramDeclarations.add(String.format("{@%s %s%s}",
                                pd.getTypeInfo().substring(1, pd.getTypeInfo().length() - 1), pd.getKey(),
                                pd.getDefaultValue() != null ? "=" + pd.getDefaultValue().toOriginalString() : ""));
                    }
                    template.put("paramDeclarations", paramDeclarations);
                }
            }
            templates.add(template);
        }
        return templates;
    }

    private String getBasePath(String path, TemplateVariantsBuildItem variants) {
        for (Entry<String, List<String>> e : variants.getVariants().entrySet()) {
            if (e.getValue().contains(path)) {
                return e.getKey();
            }
        }
        return null;
    }

    private CheckedTemplateBuildItem findCheckedTemplate(String basePath, List<CheckedTemplateBuildItem> checkedTemplates) {
        if (basePath != null) {
            for (CheckedTemplateBuildItem checkedTemplate : checkedTemplates) {
                if (checkedTemplate.isFragment()) {
                    continue;
                }
                if (checkedTemplate.templateId.equals(basePath)) {
                    return checkedTemplate;
                }
            }
        }
        return null;
    }

}

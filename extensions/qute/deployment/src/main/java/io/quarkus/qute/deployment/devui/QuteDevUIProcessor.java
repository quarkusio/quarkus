package io.quarkus.qute.deployment.devui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.qute.ParameterDeclaration;
import io.quarkus.qute.deployment.CheckedTemplateBuildItem;
import io.quarkus.qute.deployment.EffectiveTemplatePathsBuildItem;
import io.quarkus.qute.deployment.ImplicitValueResolverBuildItem;
import io.quarkus.qute.deployment.TemplateDataBuildItem;
import io.quarkus.qute.deployment.TemplateExtensionMethodBuildItem;
import io.quarkus.qute.deployment.TemplateGlobalBuildItem;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.qute.deployment.TemplateVariantsBuildItem;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.smallrye.common.annotation.SuppressForbidden;

public class QuteDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void pages(
            EffectiveTemplatePathsBuildItem effectiveTemplatePaths,
            List<CheckedTemplateBuildItem> checkedTemplates,
            TemplateVariantsBuildItem variants,
            TemplatesAnalysisBuildItem templatesAnalysis,
            List<TemplateExtensionMethodBuildItem> templateExtensionMethods,
            List<TemplateDataBuildItem> templateDatas,
            List<ImplicitValueResolverBuildItem> implicitTemplateDatas,
            List<TemplateGlobalBuildItem> templateGlobals,
            BuildProducer<CardPageBuildItem> cardPages) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        List<TemplatePathBuildItem> sortedTemplatePaths = effectiveTemplatePaths.getTemplatePaths().stream()
                .sorted(Comparator.comparing(tp -> tp.getPath().toLowerCase())).collect(Collectors.toList());
        pageBuildItem.addBuildTimeData("templates",
                createTemplatesJson(sortedTemplatePaths, checkedTemplates, templatesAnalysis, variants));

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
        pageBuildItem.addBuildTimeData("extensionMethods", createExtensionMethodsJson(sortedExtensionMethods));

        List<TemplateDataBuildItem> sortedTemplateData = new ArrayList<>(templateDatas);
        Set<DotName> explicitTargets = new HashSet<>();
        for (TemplateDataBuildItem td : templateDatas) {
            explicitTargets.add(td.getTargetClass().name());
        }
        for (ImplicitValueResolverBuildItem itd : implicitTemplateDatas) {
            if (!explicitTargets.contains(itd.getClazz().name())) {
                sortedTemplateData.add(new TemplateDataBuildItem(itd.getTemplateData(), itd.getClazz()));
            }
        }
        sortedTemplateData = sortedTemplateData.stream()
                .sorted(Comparator.comparing(td -> td.getTargetClass().name())).collect(Collectors.toList());
        if (!sortedTemplateData.isEmpty()) {
            pageBuildItem.addBuildTimeData("templateData", createTemplateDataJson(sortedTemplateData));
        }

        List<TemplateGlobalBuildItem> sortedTemplateGlobals = templateGlobals.stream()
                .sorted(Comparator.comparing(tg -> tg.getName().toLowerCase())).collect(Collectors.toList());
        if (!sortedTemplateGlobals.isEmpty()) {
            pageBuildItem.addBuildTimeData("templateGlobals", createTemplateGlobalsJson(sortedTemplateGlobals));
        }

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Templates")
                .icon("font-awesome-solid:file-code")
                .componentLink("qwc-qute-templates.js")
                .staticLabel(String.valueOf(sortedTemplatePaths.size())));

        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Extension Methods")
                .icon("font-awesome-solid:puzzle-piece")
                .componentLink("qwc-qute-extension-methods.js")
                .staticLabel(String.valueOf(sortedExtensionMethods.size())));

        if (!sortedTemplateData.isEmpty()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .title("Template Data")
                    .icon("font-awesome-solid:database")
                    .componentLink("qwc-qute-template-data.js")
                    .staticLabel(String.valueOf(sortedTemplateData.size())));
        }

        if (!sortedTemplateGlobals.isEmpty()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .title("Global Variables")
                    .icon("font-awesome-solid:globe")
                    .componentLink("qwc-qute-template-globals.js")
                    .staticLabel(String.valueOf(sortedTemplateGlobals.size())));
        }

        cardPages.produce(pageBuildItem);
    }

    private List<Map<String, String>> createTemplateGlobalsJson(List<TemplateGlobalBuildItem> sortedTemplateGlobals) {
        List<Map<String, String>> globals = new ArrayList<>();
        for (TemplateGlobalBuildItem global : sortedTemplateGlobals) {
            Map<String, String> map = new HashMap<>();
            map.put("name", global.getName());
            map.put("target", global.getDeclaringClass() + "#"
                    + (global.isField() ? global.getTarget().asField().name() : global.getTarget().asMethod().name() + "()"));
            globals.add(map);
        }
        return globals;
    }

    private List<Map<String, Object>> createTemplateDataJson(List<TemplateDataBuildItem> sortedTemplateData) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (TemplateDataBuildItem templateData : sortedTemplateData) {
            Map<String, Object> map = new HashMap<>();
            map.put("target", templateData.getTargetClass().name().toString());
            if (templateData.hasNamespace()) {
                map.put("namespace", templateData.getNamespace());
            }
            if (templateData.getIgnore() != null && templateData.getIgnore().length > 0) {
                map.put("ignores", Arrays.toString(templateData.getIgnore()));
            }
            if (templateData.isProperties()) {
                map.put("properties", true);
            }
            data.add(map);
        }
        return data;
    }

    @SuppressForbidden(reason = "Type#toString() is what we want to use here")
    private List<Map<String, String>> createExtensionMethodsJson(
            List<TemplateExtensionMethodBuildItem> sortedExtensionMethods) {
        List<Map<String, String>> extensionMethods = new ArrayList<>();
        for (TemplateExtensionMethodBuildItem templateExtensionMethod : sortedExtensionMethods) {
            Map<String, String> extensionMethod = new HashMap<>();
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

    private List<Map<String, Object>> createTemplatesJson(List<TemplatePathBuildItem> sortedTemplatePaths,
            List<CheckedTemplateBuildItem> checkedTemplates, TemplatesAnalysisBuildItem templatesAnalysis,
            TemplateVariantsBuildItem variants) {
        List<Map<String, Object>> templates = new ArrayList<>();
        for (TemplatePathBuildItem templatePath : sortedTemplatePaths) {
            Map<String, Object> template = new HashMap<>();
            template.put("path", templatePath.getPath());

            CheckedTemplateBuildItem checkedTemplate = findCheckedTemplate(getBasePath(templatePath.getPath(), variants),
                    checkedTemplates);
            if (checkedTemplate != null) {
                template.put("checkedTemplate",
                        checkedTemplate.getDescription());
            }

            TemplateAnalysis analysis = templatesAnalysis.getAnalysis().stream()
                    .filter(ta -> ta.path.equals(templatePath.getPath())).findFirst().orElse(null);
            if (analysis != null) {
                if (!analysis.fragmentIds.isEmpty()) {
                    List<String> fragmentIds = new ArrayList<>();
                    analysis.fragmentIds.forEach(fragmentIds::add);
                    template.put("fragmentIds", fragmentIds);
                }
                if (!analysis.parameterDeclarations.isEmpty()) {
                    List<String> paramDeclarations = new ArrayList<>();
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

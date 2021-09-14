package io.quarkus.qute.deployment.devconsole;

import java.util.List;
import java.util.Map.Entry;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.qute.deployment.CheckedTemplateBuildItem;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.qute.deployment.TemplateVariantsBuildItem;

public class QuteDevConsoleProcessor {

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
                        entry.getValue(),
                        checkedTemplate.method.declaringClass().name() + "." + checkedTemplate.method.name() + "()",
                        checkedTemplate.bindings));
            } else {
                quteInfos.addQuteTemplateInfo(new DevQuteTemplateInfo(entry.getKey(),
                        entry.getValue(),
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

}

package io.quarkus.qute.deployment.devconsole;

import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.qute.deployment.CheckedTemplateBuildItem;
import io.quarkus.qute.deployment.TemplateVariantsBuildItem;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectBeanInfo(
            List<CheckedTemplateBuildItem> checkedTemplates,
            TemplateVariantsBuildItem variants) {
        DevQuteInfos quteInfos = new DevQuteInfos();
        for (CheckedTemplateBuildItem checkedTemplate : checkedTemplates) {
            DevQuteTemplateInfo templateInfo = new DevQuteTemplateInfo(checkedTemplate.templateId,
                    variants.getVariants().get(checkedTemplate.templateId),
                    checkedTemplate.bindings);
            quteInfos.addQuteTemplateInfo(templateInfo);
        }
        return new DevConsoleTemplateInfoBuildItem("devQuteInfos", quteInfos);

    }

}

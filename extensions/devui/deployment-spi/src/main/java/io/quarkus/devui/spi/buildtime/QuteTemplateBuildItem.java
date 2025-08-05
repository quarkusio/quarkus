package io.quarkus.devui.spi.buildtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Contains info on the build time template used to build static content for Dev UI
 * All files are relative to dev-ui-templates/build-time/{extensionName} (in src/main/resources)
 *
 * This contain the fileName to the template, and the template data (variables)
 *
 * This allows extensions developers to add "static files" that they generate with Qute at build time.
 * From a runtime p.o.v this is file served from "disk"
 */
public final class QuteTemplateBuildItem extends AbstractDevUIBuildItem {
    private final List<TemplateData> templateDatas;

    public QuteTemplateBuildItem() {
        super();
        this.templateDatas = new ArrayList<>();
    }

    public QuteTemplateBuildItem(String customIdentifier) {
        super(customIdentifier);
        this.templateDatas = new ArrayList<>();
    }

    public List<TemplateData> getTemplateDatas() {
        return templateDatas;
    }

    public void add(String templatename, Map<String, Object> data) {
        templateDatas.add(new TemplateData(templatename, templatename, data, Map.of())); // By default the template is used for only one file.
    }

    public void add(String templatename, String fileName, Map<String, Object> data, Map<String, String> descriptions) {
        templateDatas.add(new TemplateData(templatename, fileName, data, descriptions));
    }

    public static class TemplateData {
        final String templateName;
        final String fileName;
        final Map<String, Object> data;
        final Map<String, String> descriptions;

        private TemplateData(String templateName, String fileName, Map<String, Object> data, Map<String, String> descriptions) {
            this.templateName = templateName;
            this.fileName = fileName;
            this.data = data;
            this.descriptions = descriptions;
        }

        public String getTemplateName() {
            return templateName;
        }

        public String getFileName() {
            return fileName;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public Map<String, String> getDescriptions() {
            return descriptions;
        }
    }
}

package io.quarkus.qute.deployment.devconsole;

import java.util.ArrayList;
import java.util.List;

public class DevQuteInfos {
    private List<DevQuteTemplateInfo> templateInfos = new ArrayList<>();

    public DevQuteInfos() {
    }

    public List<DevQuteTemplateInfo> getTemplateInfos() {
        return templateInfos;
    }

    public void setTemplateInfos(List<DevQuteTemplateInfo> templateInfos) {
        this.templateInfos = templateInfos;
    }

    public void addQuteTemplateInfo(DevQuteTemplateInfo info) {
        templateInfos.add(info);
    }
}

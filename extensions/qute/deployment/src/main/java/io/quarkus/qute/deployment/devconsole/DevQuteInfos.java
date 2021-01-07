package io.quarkus.qute.deployment.devconsole;

import java.util.ArrayList;
import java.util.List;

public class DevQuteInfos {

    private final List<DevQuteTemplateInfo> templateInfos;

    public DevQuteInfos() {
        templateInfos = new ArrayList<>();
    }

    public List<DevQuteTemplateInfo> getTemplates() {
        return templateInfos;
    }

    public void addQuteTemplateInfo(DevQuteTemplateInfo info) {
        templateInfos.add(info);
    }
}

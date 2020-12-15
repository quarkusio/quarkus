package io.quarkus.arc.deployment.devconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevBeanInfos {
    private List<DevBeanInfo> beanInfos = new ArrayList<>();
    private List<DevBeanInfo> removedBeanInfos = new ArrayList<>();

    public DevBeanInfos() {
    }

    public void setRemovedBeanInfos(List<DevBeanInfo> removedBeanInfos) {
        this.removedBeanInfos = removedBeanInfos;
    }

    public List<DevBeanInfo> getRemovedBeanInfos() {
        Collections.sort(removedBeanInfos);
        return removedBeanInfos;
    }

    public List<DevBeanInfo> getBeanInfos() {
        Collections.sort(beanInfos);
        return beanInfos;
    }

    public void setBeanInfos(List<DevBeanInfo> beanInfos) {
        this.beanInfos = beanInfos;
    }

    public void addBeanInfo(DevBeanInfo beanInfo) {
        beanInfos.add(beanInfo);
    }

    public void addRemovedBeanInfo(DevBeanInfo beanInfo) {
        removedBeanInfos.add(beanInfo);
    }
}

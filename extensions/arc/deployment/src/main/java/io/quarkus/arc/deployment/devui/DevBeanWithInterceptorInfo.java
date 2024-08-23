package io.quarkus.arc.deployment.devui;

import java.util.ArrayList;
import java.util.List;

public class DevBeanWithInterceptorInfo extends DevBeanInfo {

    private final List<DevInterceptorInfo> interceptorInfos = new ArrayList<>();

    public DevBeanWithInterceptorInfo(DevBeanInfo beanInfo, DevBeanInfos beanInfos) {
        super(beanInfo.getId(), beanInfo.getKind(), beanInfo.isApplicationBean(), beanInfo.getProviderType(),
                beanInfo.getMemberName(), beanInfo.getTypes(), beanInfo.getQualifiers(), beanInfo.getScope(),
                beanInfo.getDeclaringClass(), beanInfo.getInterceptors(), beanInfo.isGenerated());

        for (String interceptorId : beanInfo.getInterceptors()) {
            this.interceptorInfos.add(beanInfos.getInterceptor(interceptorId));
        }
    }

    public List<DevInterceptorInfo> getInterceptorInfos() {
        return interceptorInfos;
    }

}

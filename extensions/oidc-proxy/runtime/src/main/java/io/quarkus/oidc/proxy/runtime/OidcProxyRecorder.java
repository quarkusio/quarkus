package io.quarkus.oidc.proxy.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.Router;

@Recorder
public class OidcProxyRecorder {

    final RuntimeValue<OidcProxyConfig> oidcProxyConfig;

    public OidcProxyRecorder(RuntimeValue<OidcProxyConfig> oidcProxyConfig) {
        this.oidcProxyConfig = oidcProxyConfig;
    }

    public void setupRoutes(BeanContainer beanContainer, RuntimeValue<Router> routerValue, String httpRootPath) {
        TenantConfigBean oidcTenantBean = beanContainer.beanInstance(TenantConfigBean.class);
        OidcProxy proxy = new OidcProxy(oidcTenantBean, oidcProxyConfig.getValue());
        Router router = routerValue.getValue();
        proxy.setup(router, httpRootPath);
    }
}

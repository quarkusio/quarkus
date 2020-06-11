package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJtaPlatformInitiator implements StandardServiceInitiator<JtaPlatform> {

    private final boolean jtaIsPresent;

    public QuarkusJtaPlatformInitiator(boolean jtaIsPresent) {
        this.jtaIsPresent = jtaIsPresent;
    }

    @Override
    public JtaPlatform initiateService(Map map, ServiceRegistryImplementor serviceRegistryImplementor) {
        return buildJtaPlatformInstance();
    }

    public JtaPlatform buildJtaPlatformInstance() {
        return jtaIsPresent ? getJtaInstance() : getNoJtaInstance();
    }

    private NoJtaPlatform getNoJtaInstance() {
        return NoJtaPlatform.INSTANCE;
    }

    private QuarkusJtaPlatform getJtaInstance() {
        return QuarkusJtaPlatform.INSTANCE;
    }

    @Override
    public Class<JtaPlatform> getServiceInitiated() {
        return JtaPlatform.class;
    }

}

package io.quarkus.hibernate.rx.runtime.customized;

import org.hibernate.cfg.AvailableSettings;

// TODO: move this to HibernateRX
public interface AvailableRxSettings extends AvailableSettings {

    String VERTX_POOL = "hibernate.rx.connection.vertx.pool";

}

package io.quarkus.hibernate.rx.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateRxRecorder {

    /**
     * The feature needs to be initialized, even if it's not enabled.
     *
     * @param enabled Set to false if it's not being enabled, to log appropriately.
     */
    public void callHibernateRxFeatureInit(boolean enabled) {
        HibernateRx.featureInit(enabled);
    }

}

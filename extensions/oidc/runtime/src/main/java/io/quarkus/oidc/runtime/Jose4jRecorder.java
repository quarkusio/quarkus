package io.quarkus.oidc.runtime;

import org.jose4j.jwa.AlgorithmFactoryFactory;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class Jose4jRecorder {

    public void initialize() {
        AlgorithmFactoryFactory.getInstance();
    }

}

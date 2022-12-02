package io.quarkus.narayana.jta.runtime.graal;

import java.util.Properties;

import com.arjuna.common.util.propertyservice.PropertiesFactory;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.narayana.jta.runtime.NarayanaJtaRecorder;

@TargetClass(PropertiesFactory.class)
final class PropertiesFactorySubstitution {

    @Substitute
    public static Properties getDefaultProperties() {
        return NarayanaJtaRecorder.getDefaultProperties();
    }

}

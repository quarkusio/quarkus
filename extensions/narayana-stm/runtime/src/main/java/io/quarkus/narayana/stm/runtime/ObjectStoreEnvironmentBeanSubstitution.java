package io.quarkus.narayana.stm.runtime;

import java.io.File;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean")
public final class ObjectStoreEnvironmentBeanSubstitution {

    /**
     * @return fixed ObjectStore path resolved during runtime
     */
    @Substitute
    public String getObjectStoreDir() {
        return System.getProperty("user.home") + File.separator + "ObjectStore";
    }
}

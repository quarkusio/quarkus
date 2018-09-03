package org.jboss.shamrock.transactions.runtime.graal;

import java.util.Properties;

import org.jboss.shamrock.transactions.runtime.TransactionTemplate;

import com.arjuna.common.util.propertyservice.PropertiesFactory;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(PropertiesFactory.class)
final class PropertiesFactorySubstitution {

    @Substitute
    public static Properties getDefaultProperties() {
        return TransactionTemplate.getDefaultProperties();
    }

}

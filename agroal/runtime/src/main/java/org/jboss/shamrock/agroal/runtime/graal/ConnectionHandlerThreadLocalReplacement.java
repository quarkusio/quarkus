package org.jboss.shamrock.agroal.runtime.graal;

import java.util.ArrayList;
import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.agroal.pool.ConnectionHandler;

//workaround for https://github.com/oracle/graal/issues/654
@TargetClass(className = "io.agroal.pool.ConnectionPool$ConnectionHandlerThreadLocal")
final class ConnectionHandlerThreadLocalReplacement {

    @Substitute
    protected List<ConnectionHandler> initialValue() {
        return new ArrayList<>();
    }

}

package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheHibernateRecorder {
    public void setNamedQueryMap(Map<String, Set<String>> namedQueryMap) {
        NamedQueryUtil.setNamedQueryMap(namedQueryMap);
    }

    public void clear(ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                SessionOperations.clear();
            }
        });
    }
}

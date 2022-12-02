package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheHibernateRecorder {
    public void setNamedQueryMap(Map<String, Set<String>> namedQueryMap) {
        NamedQueryUtil.setNamedQueryMap(namedQueryMap);
    }
}

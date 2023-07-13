package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheHibernateRecorder {
    public void setNamedQueryMap(Map<String, Map<String, String>> namedQueryMap) {
        NamedQueryUtil.setNamedQueryMap(namedQueryMap);
    }
}

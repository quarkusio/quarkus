package io.quarkus.arc.test.interceptors.postConstruct.inherited.subpackage;

import io.quarkus.arc.test.interceptors.postConstruct.inherited.OriginalBean;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@ApplicationScoped
@Priority(1)
public class AlternativeBean extends OriginalBean {

    public String ping() {
        return AlternativeBean.class.getSimpleName();
    }
}

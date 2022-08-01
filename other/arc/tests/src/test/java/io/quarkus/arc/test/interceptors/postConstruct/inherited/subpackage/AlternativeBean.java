package io.quarkus.arc.test.interceptors.postConstruct.inherited.subpackage;

import io.quarkus.arc.test.interceptors.postConstruct.inherited.OriginalBean;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

@Alternative
@ApplicationScoped
@Priority(1)
public class AlternativeBean extends OriginalBean {

    public String ping() {
        return AlternativeBean.class.getSimpleName();
    }
}

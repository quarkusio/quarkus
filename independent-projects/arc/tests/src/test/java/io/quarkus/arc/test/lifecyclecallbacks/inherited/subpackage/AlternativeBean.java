package io.quarkus.arc.test.lifecyclecallbacks.inherited.subpackage;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import io.quarkus.arc.test.lifecyclecallbacks.inherited.OriginalBean;

@Alternative
@ApplicationScoped
@Priority(1)
public class AlternativeBean extends OriginalBean {

    public String ping() {
        return AlternativeBean.class.getSimpleName();
    }
}

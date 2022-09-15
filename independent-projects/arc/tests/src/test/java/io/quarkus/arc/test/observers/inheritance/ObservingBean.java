package io.quarkus.arc.test.observers.inheritance;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 18/07/2019
 */
@ApplicationScoped
@ObservingBean.THIS
public class ObservingBean {

    public volatile String value;

    public void watchFor(@Observes SimpleEvent event) {
        value = event.content;
    }

    public String getValue() {
        return value;
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    public @interface THIS {

    }
}

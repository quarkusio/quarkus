import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class B {

    @Inject
    @ConfigProperty(name = "simpleBean.baz")
    Optional<String> bazOptional;

    @ActivateRequestContext
    String ping() {
        return bazOptional.orElse("bang!");
    }

}

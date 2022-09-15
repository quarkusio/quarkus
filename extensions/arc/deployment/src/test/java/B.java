import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

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

package io.quarkus.bootstrap.resolver;

import java.io.Serializable;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.ModelParameter;

public class QuarkusModelBuildAction implements BuildAction<ApplicationModel>, Serializable {

    private static final long serialVersionUID = 9152408068581769671L;

    private final String mode;

    public QuarkusModelBuildAction(String mode) {
        this.mode = mode;
    }

    @Override
    public ApplicationModel execute(BuildController controller) {
        return controller.getModel(ApplicationModel.class, ModelParameter.class, p -> p.setMode(mode));
    }
}

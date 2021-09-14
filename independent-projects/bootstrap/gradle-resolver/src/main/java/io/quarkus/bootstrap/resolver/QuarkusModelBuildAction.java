package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import java.io.Serializable;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;

public class QuarkusModelBuildAction implements BuildAction<QuarkusModel>, Serializable {
    private final String mode;

    public QuarkusModelBuildAction(String mode) {
        this.mode = mode;
    }

    @Override
    public QuarkusModel execute(BuildController controller) {
        return controller.getModel(QuarkusModel.class, ModelParameter.class, p -> p.setMode(mode));
    }
}

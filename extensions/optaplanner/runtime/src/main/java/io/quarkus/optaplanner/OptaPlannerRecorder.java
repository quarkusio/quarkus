package io.quarkus.optaplanner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;

@Recorder
public class OptaPlannerRecorder {

    public BeanContainerListener initializeSolverFactory(Class<?> solutionClass, List<Class<?>> entityClassList,
            Class<? extends ConstraintProvider> constraintProviderClass) {
        return container -> {
            SolverConfig solverConfig = new SolverConfig();
            solverConfig.setSolutionClass(solutionClass);
            solverConfig.setEntityClassList(entityClassList);
            solverConfig.setScoreDirectorFactoryConfig(
                    new ScoreDirectorFactoryConfig()
                            // Use Bavet to avoid Drools classpath issues (drools 7 vs kogito 1 code duplication)
                            .withConstraintStreamImplType(ConstraintStreamImplType.BAVET)
                            .withConstraintProviderClass(constraintProviderClass));
            SolverFactoryProvider.solverFactory = SolverFactory.create(solverConfig);
        };
    }

}

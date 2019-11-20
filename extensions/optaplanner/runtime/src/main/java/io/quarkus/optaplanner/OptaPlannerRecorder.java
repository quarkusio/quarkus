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

    public BeanContainerListener initializeSolverFactory(String solutionClassName, List<String> entityClassNameList,
            String constraintProviderClassName) {
        return container -> {
            SolverConfig solverConfig = new SolverConfig();
            try {
                Class<?> solutionClass = Class.forName(solutionClassName);
                solverConfig.setSolutionClass(solutionClass);

                List<Class<?>> entityClassList = new ArrayList<>(entityClassNameList.size());
                for (String entityClassName : entityClassNameList) {
                    entityClassList.add(Class.forName(entityClassName));
                }
                solverConfig.setEntityClassList(entityClassList);

                Class<? extends ConstraintProvider> constraintProviderClass =
                        Class.forName(constraintProviderClassName).asSubclass(ConstraintProvider.class);
                solverConfig.setScoreDirectorFactoryConfig(
                        new ScoreDirectorFactoryConfig()
                                // Use Bavet to avoid Drools classpath issues (drools 7 vs kogito 1 code duplication)
                                .withConstraintStreamImplType(ConstraintStreamImplType.BAVET)
                                .withConstraintProviderClass(constraintProviderClass));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "One of the classes cannot be resolved in this Quarkus runtime, "
                        + "but they were resolved correctly in the Quarkus deployment extension.", e);
            }
            SolverFactoryProvider.solverFactory = SolverFactory.create(solverConfig);
        };
    }

}

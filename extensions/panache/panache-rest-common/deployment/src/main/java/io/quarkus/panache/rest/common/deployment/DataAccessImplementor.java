package io.quarkus.panache.rest.common.deployment;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public interface DataAccessImplementor {

    ResultHandle findById(BytecodeCreator creator, ResultHandle id);

    ResultHandle listAll(BytecodeCreator creator);

    ResultHandle persist(BytecodeCreator creator, ResultHandle entity);

    ResultHandle update(BytecodeCreator creator, ResultHandle entity);

    ResultHandle deleteById(BytecodeCreator creator, ResultHandle id);
}

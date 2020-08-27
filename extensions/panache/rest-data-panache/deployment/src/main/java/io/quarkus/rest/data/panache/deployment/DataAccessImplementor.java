package io.quarkus.rest.data.panache.deployment;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;

public interface DataAccessImplementor {

    ResultHandle findById(BytecodeCreator creator, ResultHandle id);

    ResultHandle listAll(BytecodeCreator creator, ResultHandle sort);

    ResultHandle findAll(BytecodeCreator creator, ResultHandle page, ResultHandle sort);

    ResultHandle persist(BytecodeCreator creator, ResultHandle entity);

    ResultHandle update(BytecodeCreator creator, ResultHandle entity);

    ResultHandle deleteById(BytecodeCreator creator, ResultHandle id);

    ResultHandle pageCount(BytecodeCreator creator, ResultHandle page);
}

package io.quarkus.redis.lettuce.runtime.internal;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.quarkus.redis.datasource.ReactiveCursor;

public abstract class AbstractReactiveScanCursor<T> implements ReactiveCursor<T> {

    protected final LettuceConnectionSource source;
    protected final ScanArgs scanArgs;
    protected ScanCursor cursor = ScanCursor.INITIAL;

    protected AbstractReactiveScanCursor(LettuceConnectionSource source, ScanArgs scanArgs) {
        nonNull(source, "source");
        nonNull(scanArgs, "scanArgs");
        this.source = source;
        this.scanArgs = scanArgs;
    }

    @Override
    public boolean hasNext() {
        return !cursor.isFinished();
    }

    @Override
    public long cursorId() {
        return Long.parseUnsignedLong(cursor.getCursor());
    }

}

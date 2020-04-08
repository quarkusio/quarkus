package io.quarkus.optaplanner.jsonb;

import javax.inject.Singleton;
import javax.json.bind.JsonbConfig;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.persistence.jsonb.api.score.buildin.bendable.BendableScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.bendablelong.BendableLongScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.hardmediumsoft.HardMediumSoftScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.hardsoft.HardSoftScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.hardsoftdouble.HardSoftDoubleScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.hardsoftlong.HardSoftLongScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.simple.SimpleScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.simpledouble.SimpleDoubleScoreJsonbAdapter;
import org.optaplanner.persistence.jsonb.api.score.buildin.simplelong.SimpleLongScoreJsonbAdapter;

import io.quarkus.jsonb.JsonbConfigCustomizer;

/**
 * OptaPlanner doesn't use JSON-B, but it does have optional JSON-B support for {@link Score}, etc.
 */
@Singleton
public class OptaPlannerJsonbConfigCustomizer implements JsonbConfigCustomizer {

    @Override
    public void customize(JsonbConfig config) {
        // TODO when upgrading to OptaPlanner 7.36.0.Final use OptaPlannerJsonbConfig.getScoreJsonbAdapters() instead
        config.withAdapters(new BendableScoreJsonbAdapter(),
                new BendableBigDecimalScoreJsonbAdapter(),
                new BendableLongScoreJsonbAdapter(),
                new HardMediumSoftScoreJsonbAdapter(),
                new HardMediumSoftBigDecimalScoreJsonbAdapter(),
                new HardMediumSoftLongScoreJsonbAdapter(),
                new HardSoftScoreJsonbAdapter(),
                new HardSoftBigDecimalScoreJsonbAdapter(),
                new HardSoftDoubleScoreJsonbAdapter(),
                new HardSoftLongScoreJsonbAdapter(),
                new SimpleScoreJsonbAdapter(),
                new SimpleBigDecimalScoreJsonbAdapter(),
                new SimpleDoubleScoreJsonbAdapter(),
                new SimpleLongScoreJsonbAdapter());
    }
}

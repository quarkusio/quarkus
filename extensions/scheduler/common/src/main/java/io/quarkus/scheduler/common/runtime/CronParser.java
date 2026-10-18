package io.quarkus.scheduler.common.runtime;

import static com.cronutils.model.field.expression.FieldExpression.questionMark;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import com.cronutils.Function;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.SingleCron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.Always;
import com.cronutils.model.field.expression.QuestionMark;

public class CronParser {

    private final CronType cronType;

    private final com.cronutils.parser.CronParser cronParser;

    public CronParser(CronType cronType) {
        this.cronType = cronType;
        this.cronParser = new com.cronutils.parser.CronParser(CronDefinitionBuilder.instanceDefinitionFor(cronType));
    }

    public CronType cronType() {
        return cronType;
    }

    public Cron parse(String value) {
        return cronParser.parse(value);
    }

    public Cron mapToQuartz(Cron cron) {
        switch (cronType) {
            case QUARTZ:
                return cron;
            case UNIX:
                return CronMapper.fromUnixToQuartz().map(cron);
            case CRON4J:
                return CronMapper.fromCron4jToQuartz().map(cron);
            case SPRING:
                return CronMapper.fromSpringToQuartz().map(cron);
            case SPRING53:
                // https://github.com/jmrozanec/cron-utils/issues/579
                return new CronMapper(
                        CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING53),
                        CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ),
                        setQuestionMark()).map(cron);
            default:
                throw new IllegalStateException("Unsupported cron type: " + cronType);
        }
    }

    // Copy from com.cronutils.mapper.CronMapper#setQuestionMark()
    private static Function<Cron, Cron> setQuestionMark() {
        return cron -> {
            final CronField dow = cron.retrieve(CronFieldName.DAY_OF_WEEK);
            final CronField dom = cron.retrieve(CronFieldName.DAY_OF_MONTH);
            if (dow == null && dom == null) {
                return cron;
            }
            if (dow.getExpression() instanceof QuestionMark || dom.getExpression() instanceof QuestionMark) {
                return cron;
            }
            final Map<CronFieldName, CronField> fields = new EnumMap<>(CronFieldName.class);
            fields.putAll(cron.retrieveFieldsAsMap());
            if (dow.getExpression() instanceof Always) {
                fields.put(CronFieldName.DAY_OF_WEEK,
                        new CronField(CronFieldName.DAY_OF_WEEK, questionMark(),
                                fields.get(CronFieldName.DAY_OF_WEEK).getConstraints()));
            } else {
                if (dom.getExpression() instanceof Always) {
                    fields.put(CronFieldName.DAY_OF_MONTH,
                            new CronField(CronFieldName.DAY_OF_MONTH, questionMark(),
                                    fields.get(CronFieldName.DAY_OF_MONTH).getConstraints()));
                } else {
                    cron.validate();
                }
            }
            return new SingleCron(cron.getCronDefinition(), new ArrayList<>(fields.values()));
        };
    }

}

package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.Constants.EMPTY;
import static io.quarkus.annotation.processor.Constants.NEW_LINE;
import static io.quarkus.annotation.processor.Constants.SECTION_TITLE_L1;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.annotation.processor.Constants;

public final class MavenConfigDocBuilder extends ConfigDocBuilder {

    public MavenConfigDocBuilder() {
        super(false);
    }

    private final JavaDocParser javaDocParser = new JavaDocParser();

    public void addTableTitle(String goalTitle) {
        write(SECTION_TITLE_L1, goalTitle, NEW_LINE);
    }

    public void addNewLine() {
        write(NEW_LINE);
    }

    public void addTableDescription(String goalDescription) {
        write(NEW_LINE, javaDocParser.parseConfigDescription(goalDescription), NEW_LINE);
    }

    public GoalParamsBuilder newGoalParamsBuilder() {
        return new GoalParamsBuilder(javaDocParser);
    }

    private static abstract class TableBuilder {

        protected final List<ConfigDocItem> configDocItems = new ArrayList<>();

        /**
         * Section name that is displayed in a table header
         */
        abstract protected String getSectionName();

        public List<ConfigDocItem> build() {

            // a summary table
            final ConfigDocSection parameterSection = new ConfigDocSection();
            parameterSection.setShowSection(true);
            parameterSection.setName(getSectionName());
            parameterSection.setSectionDetailsTitle(getSectionName());
            parameterSection.setOptional(false);
            parameterSection.setConfigDocItems(List.copyOf(configDocItems));

            // topConfigDocItem wraps the summary table
            final ConfigDocItem topConfigDocItem = new ConfigDocItem();
            topConfigDocItem.setConfigDocSection(parameterSection);

            return List.of(topConfigDocItem);
        }

        public boolean tableIsNotEmpty() {
            return !configDocItems.isEmpty();
        }
    }

    public static final class GoalParamsBuilder extends TableBuilder {

        private final JavaDocParser javaDocParser;

        private GoalParamsBuilder(JavaDocParser javaDocParser) {
            this.javaDocParser = javaDocParser;
        }

        public void addParam(String type, String name, String defaultValue, boolean required, String description) {
            final ConfigDocKey configDocKey = new ConfigDocKey();
            configDocKey.setType(type);
            configDocKey.setKey(name);
            configDocKey.setAdditionalKeys(List.of(name));
            configDocKey.setConfigPhase(ConfigPhase.RUN_TIME);
            configDocKey.setDefaultValue(defaultValue == null ? Constants.EMPTY : defaultValue);
            if (description != null && !description.isBlank()) {
                configDocKey.setConfigDoc(javaDocParser.parseConfigDescription(description));
            } else {
                configDocKey.setConfigDoc(EMPTY);
            }
            configDocKey.setOptional(!required);
            final ConfigDocItem configDocItem = new ConfigDocItem();
            configDocItem.setConfigDocKey(configDocKey);
            configDocItems.add(configDocItem);
        }

        @Override
        protected String getSectionName() {
            return "Parameter";
        }
    }

}

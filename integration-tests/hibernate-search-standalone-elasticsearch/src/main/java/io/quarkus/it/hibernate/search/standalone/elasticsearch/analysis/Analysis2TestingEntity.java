package io.quarkus.it.hibernate.search.standalone.elasticsearch.analysis;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity
@Indexed
public class Analysis2TestingEntity extends AnalysisTestingEntityBase {
    public Analysis2TestingEntity(long id, String text) {
        super(id, text);
    }

    @FullTextField(analyzer = "index-level-analyzer-2")
    @Override
    public String getText() {
        return super.getText();
    }
}

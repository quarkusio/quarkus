package io.quarkus.it.hibernate.search.orm.elasticsearch.analysis;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Analysis3TestingEntity extends AnalysisTestingEntityBase {
    public Analysis3TestingEntity() {
    }

    public Analysis3TestingEntity(String text) {
        super(text);
    }

    @FullTextField(analyzer = "index-level-analyzer-3")
    @Override
    public String getText() {
        return super.getText();
    }
}

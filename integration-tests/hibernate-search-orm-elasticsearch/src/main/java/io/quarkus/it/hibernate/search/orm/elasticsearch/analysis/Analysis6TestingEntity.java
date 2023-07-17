package io.quarkus.it.hibernate.search.orm.elasticsearch.analysis;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Analysis6TestingEntity extends AnalysisTestingEntityBase {
    public Analysis6TestingEntity() {
    }

    public Analysis6TestingEntity(String text) {
        super(text);
    }

    @FullTextField(analyzer = "index-level-analyzer-6")
    @Override
    public String getText() {
        return super.getText();
    }
}

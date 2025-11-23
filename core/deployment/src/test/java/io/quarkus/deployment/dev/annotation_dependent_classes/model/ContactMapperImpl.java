package io.quarkus.deployment.dev.annotation_dependent_classes.model;

public class ContactMapperImpl implements ContactMapper {
    private MapperHelperImpl mapperHelper = new MapperHelperImpl();

    private DefaultEmailCreator defaultEmailCreator;

    @Override
    public void mapToData(Address contact) {
    }
}

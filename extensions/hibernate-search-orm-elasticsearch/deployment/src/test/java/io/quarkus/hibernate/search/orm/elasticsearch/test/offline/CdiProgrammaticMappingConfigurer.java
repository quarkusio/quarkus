package io.quarkus.hibernate.search.orm.elasticsearch.test.offline;

import io.quarkus.hibernate.search.orm.elasticsearch.SearchExtension;

@SearchExtension(persistenceUnit = "mapped")
class CdiProgrammaticMappingConfigurer extends ProgrammaticMappingConfigurer {
}

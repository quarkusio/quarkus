import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Indexe automaticamente suas entidades Hibernate no Elasticsearch.',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Tipos de Entidades Indexadas',
    'quarkus-hibernate-search-orm-elasticsearch-loading': 'Carregando...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': 'Nenhuma unidade de persistência foi encontrada.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': 'Unidade de Persistência',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities': 'Nenhuma entidade indexada foi encontrada.',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': 'Reindexar selecionados',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name': 'Nome da entidade',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'Nome da classe',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'Nomes de índices',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`Tipos de entidades selecionados: ${0}`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Selecione os tipos de entidade para reindexar para a unidade de persistência '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Solicitou a reindexação de tipos de entidade ${0} para a unidade de persistência '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Reindexação bem-sucedida de ${0} tipos de entidade para a unidade de persistência '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Ocorreu um erro ao reindexar os tipos de entidade ${0} para a unidade de persistência '${1}':\n${2}`,
};

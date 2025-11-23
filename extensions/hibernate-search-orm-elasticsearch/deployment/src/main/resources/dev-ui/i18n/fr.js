import { str } from '@lit/localize';

export const templates = {
    'quarkus-hibernate-search-orm-elasticsearch-meta-description': 'Indexez automatiquement vos entités Hibernate dans Elasticsearch',
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types': 'Types d\'entités indexées',
    'quarkus-hibernate-search-orm-elasticsearch-loading': 'Chargement...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units': 'Aucune unité de persistance n\'a été trouvée.',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit': 'Unité de persistance',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities': 'Aucune entité indexée n\'a été trouvée.',
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected': 'Réindexer sélectionné',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name': 'Nom de l\'entité',
    'quarkus-hibernate-search-orm-elasticsearch-class-name': 'Nom de la classe',
    'quarkus-hibernate-search-orm-elasticsearch-index-names': 'Noms des index',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types': str`Types d'entités sélectionnés : ${0}`,
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex': str`Sélectionnez les types d'entités à réindexer pour l'unité de persistance '${0}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started': str`Reindexation demandée de ${0} types d'entités pour l'unité de persistance '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success': str`Réindexation réussie de ${0} types d'entités pour l'unité de persistance '${1}'.`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error': str`Une erreur est survenue lors de la réindexation des types d'entité ${0} pour l'unité de persistance '${1}' :\n${2}`,
};

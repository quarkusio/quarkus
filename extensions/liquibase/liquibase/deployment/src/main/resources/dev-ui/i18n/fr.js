import { str } from '@lit/localize';

export const templates = {
    'quarkus-liquibase-meta-description': 'Gérez les migrations de schéma de votre base de données avec Liquibase',
    'quarkus-liquibase-datasources': 'Sources de données',
    'quarkus-liquibase-loading-datasources': 'Chargement des sources de données...',
    'quarkus-liquibase-name': 'Nom',
    'quarkus-liquibase-action': 'Action',
    'quarkus-liquibase-clear-database': 'Effacer la base de données',
    'quarkus-liquibase-clear': 'Effacer',
    'quarkus-liquibase-clear-confirm': 'Cela supprimera tous les objets (tables, vues, procédures, déclencheurs, ...) dans le schéma configuré. Voulez-vous continuer ?',
    'quarkus-liquibase-migrate': 'Migrer',
    'quarkus-liquibase-cleared': str`La source de données ${0} a été réinitialisée.`,
    'quarkus-liquibase-migrated': str`La source de données ${0} a été migrée.`,
};

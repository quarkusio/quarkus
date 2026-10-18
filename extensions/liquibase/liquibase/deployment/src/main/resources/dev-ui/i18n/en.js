// This is the default
import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-liquibase-meta-description':'Handle your database schema migrations with Liquibase',
    // Pages
    'quarkus-liquibase-datasources':'Datasources',
    // General
    'quarkus-liquibase-loading-datasources': 'Loading datasources...',
    'quarkus-liquibase-name': 'Name',
    'quarkus-liquibase-action': 'Action',
    'quarkus-liquibase-clear-database': 'Clear Database',
    'quarkus-liquibase-clear': 'Clear',
    'quarkus-liquibase-clear-confirm': 'This will drop all objects (tables, views, procedures, triggers, ...) in the configured schema. Do you want to continue?',
    'quarkus-liquibase-cleared': str`The datasource ${0} has been cleared.`,
    'quarkus-liquibase-migrate': 'Migrate',
    'quarkus-liquibase-migrated': str`The datasource ${0} has been migrated.`
};


// This is the default
import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-flyway-meta-description':'Handle your database schema migrations',
    // Pages
    'quarkus-flyway-datasources':'Datasources',
    // General
    'quarkus-flyway-name': 'Name',
    'quarkus-flyway-action': 'Action',
    'quarkus-flyway-clean': 'Clean',
    'quarkus-flyway-migrate': 'Migrate',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway clean has been disabled via quarkus.flyway.clean-disabled=true',
    'quarkus-flyway-update-button-title': 'Create update migration file. Always manually review the created file as it can cause data loss',
    'quarkus-flyway-generate-migration-file': 'Generate Migration File',
    'quarkus-flyway-create-button-title': 'Set up basic files for Flyway migrations to work. Initial file in db/migrations will be created and you can then add additional migration files',
    'quarkus-flyway-create-initial-migration-file': 'Create Initial Migration File',
    'quarkus-flyway-create': 'Create',
    'quarkus-flyway-update': 'Update',
    'quarkus-flyway-datasource-title': str`${0} Datasource`,
    'quarkus-flyway-create-dialog-description': 'Set up an initial file from Hibernate ORM schema generation for Flyway migrations to work.<br/>If you say yes, an initial file in <code>db/migrations</code> will be <br/>created and you can then add additional migration files as documented.',
    'quarkus-flyway-update-dialog-description': 'Create an incremental migration file from Hibernate ORM schema diff.<br/>If you say yes, an additional file in <code>db/migrations</code> will be <br/>created.',
    'quarkus-flyway-cancel': 'Cancel',
    'quarkus-flyway-clean-confirm': 'This will drop all objects (tables, views, procedures, triggers, ...) in the configured schema. Do you want to continue?'
};


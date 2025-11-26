import { str } from '@lit/localize';

export const templates = {
    'quarkus-liquibase-meta-description': 'Gestisci le migrazioni dello schema del tuo database con Liquibase',
    'quarkus-liquibase-datasources': 'Origini dati',
    'quarkus-liquibase-loading-datasources': 'Caricamento delle sorgenti dati...',
    'quarkus-liquibase-name': 'Nome',
    'quarkus-liquibase-action': 'Azione',
    'quarkus-liquibase-clear-database': 'Cancella database',
    'quarkus-liquibase-clear': 'Pulisci',
    'quarkus-liquibase-clear-confirm': 'Questo rimuoverà tutti gli oggetti (tabelle, viste, procedure, trigger, ...) nello schema configurato. Vuoi continuare?',
    'quarkus-liquibase-migrate': 'Migra',
    'quarkus-liquibase-cleared': str`Il datasource ${0} è stato svuotato.`,
    'quarkus-liquibase-migrated': str`Il datasource ${0} è stato migrato.`,
};

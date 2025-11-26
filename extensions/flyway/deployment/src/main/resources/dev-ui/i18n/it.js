import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Gestisci le migrazioni dello schema del database',
    'quarkus-flyway-datasources': 'Sorgenti dati',
    'quarkus-flyway-name': 'Nome',
    'quarkus-flyway-action': 'Azione',
    'quarkus-flyway-clean': 'Pulisci',
    'quarkus-flyway-migrate': 'Migrare',
    'quarkus-flyway-clean-disabled-tooltip': 'Flyway clean è stato disabilitato tramite quarkus.flyway.clean-disabled=true',
    'quarkus-flyway-update-button-title': 'Crea un file di migrazione di aggiornamento. Controlla sempre manualmente il file creato poiché può causare perdita di dati.',
    'quarkus-flyway-generate-migration-file': 'Genera file di migrazione',
    'quarkus-flyway-create-button-title': 'Imposta i file di base affinché le migrazioni di Flyway funzionino. Il file iniziale nella cartella db/migrations verrà creato e potrai quindi aggiungere ulteriori file di migrazione.',
    'quarkus-flyway-create-initial-migration-file': 'Crea il file di migrazione iniziale',
    'quarkus-flyway-create': 'Crea',
    'quarkus-flyway-update': 'Aggiorna',
    'quarkus-flyway-create-dialog-description': 'Configura un file iniziale per la generazione dello schema Hibernate ORM affinché le migrazioni di Flyway possano funzionare.<br/>Se dici di sì, verrà creato un file iniziale in <code>db/migrations</code> e potrai quindi aggiungere ulteriori file di migrazione come documentato.',
    'quarkus-flyway-update-dialog-description': 'Crea un file di migrazione incrementale dal diff dello schema di Hibernate ORM.<br/>Se dici di sì, verrà creato un file aggiuntivo in <code>db/migrations</code>.<br/>',
    'quarkus-flyway-cancel': 'Annulla',
    'quarkus-flyway-clean-confirm': 'Questo eliminerà tutti gli oggetti (tabelle, viste, procedure, trigger, ...) nello schema configurato. Vuoi continuare?',
    'quarkus-flyway-datasource-title': str`Datasource ${0}`,
};

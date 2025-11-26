import { str } from '@lit/localize';

export const templates = {
    'quarkus-liquibase-meta-description': 'Gestiona las migraciones de tu esquema de base de datos con Liquibase.',
    'quarkus-liquibase-datasources': 'Fuentes de datos',
    'quarkus-liquibase-loading-datasources': 'Cargando fuentes de datos...',
    'quarkus-liquibase-name': 'Nombre',
    'quarkus-liquibase-action': 'Acción',
    'quarkus-liquibase-clear-database': 'Borrar Base de Datos',
    'quarkus-liquibase-clear': 'Borrar',
    'quarkus-liquibase-clear-confirm': 'Esto eliminará todos los objetos (tablas, vistas, procedimientos, disparadores, ...) en el esquema configurado. ¿Desea continuar?',
    'quarkus-liquibase-migrate': 'Migrar',
    'quarkus-liquibase-cleared': str`La fuente de datos ${0} ha sido borrada.`,
    'quarkus-liquibase-migrated': str`La fuente de datos ${0} ha sido migrada.`,
};

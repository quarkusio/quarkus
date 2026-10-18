import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Gestiona las migraciones de tu esquema de base de datos.',
    'quarkus-flyway-datasources': 'Fuentes de datos',
    'quarkus-flyway-name': 'Nombre',
    'quarkus-flyway-action': 'Acción',
    'quarkus-flyway-clean': 'Limpiar',
    'quarkus-flyway-migrate': 'Migrar',
    'quarkus-flyway-clean-disabled-tooltip': 'La limpieza de Flyway ha sido deshabilitada mediante quarkus.flyway.clean-disabled=true',
    'quarkus-flyway-update-button-title': 'Cree un archivo de migración de actualización. Siempre revise manualmente el archivo creado, ya que puede causar pérdida de datos.',
    'quarkus-flyway-generate-migration-file': 'Generar archivo de migración',
    'quarkus-flyway-create-button-title': 'Configura los archivos básicos para que las migraciones de Flyway funcionen. Se creará un archivo inicial en db/migrations y luego podrás añadir archivos de migración adicionales.',
    'quarkus-flyway-create-initial-migration-file': 'Crear archivo de migración inicial',
    'quarkus-flyway-create': 'Crear',
    'quarkus-flyway-update': 'Actualizar',
    'quarkus-flyway-create-dialog-description': 'Configura un archivo inicial para que la generación de esquemas de Hibernate ORM funcione con las migraciones de Flyway.<br/>Si dices que sí, se creará un archivo inicial en <code>db/migrations</code> y luego podrás agregar archivos de migración adicionales según lo documentado.',
    'quarkus-flyway-update-dialog-description': 'Cree un archivo de migración incremental a partir de la diferencia del esquema de Hibernate ORM.<br/>Si dice que sí, se creará un archivo adicional en <code>db/migrations</code>.',
    'quarkus-flyway-cancel': 'Cancelar',
    'quarkus-flyway-clean-confirm': 'Esto eliminará todos los objetos (tablas, vistas, procedimientos, desencadenadores, ...) en el esquema configurado. ¿Deseas continuar?',
    'quarkus-flyway-datasource-title': str`Fuente de datos ${0}`,
};

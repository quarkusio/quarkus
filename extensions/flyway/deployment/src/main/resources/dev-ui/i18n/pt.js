import { str } from '@lit/localize';

export const templates = {
    'quarkus-flyway-meta-description': 'Gerencie as migrações do esquema do seu banco de dados.',
    'quarkus-flyway-datasources': 'Fontes de dados',
    'quarkus-flyway-name': 'Nome',
    'quarkus-flyway-action': 'Ação',
    'quarkus-flyway-clean': 'Limpar',
    'quarkus-flyway-migrate': 'Migrar',
    'quarkus-flyway-clean-disabled-tooltip': 'O Flyway clean foi desabilitado através de quarkus.flyway.clean-disabled=true.',
    'quarkus-flyway-update-button-title': 'Crie um arquivo de migração de atualização. Sempre revise manualmente o arquivo criado, pois ele pode causar perda de dados.',
    'quarkus-flyway-generate-migration-file': 'Gerar Arquivo de Migração',
    'quarkus-flyway-create-button-title': 'Configure arquivos básicos para que as migrações do Flyway funcionem. Um arquivo inicial em db/migrations será criado e você poderá adicionar arquivos de migração adicionais.',
    'quarkus-flyway-create-initial-migration-file': 'Criar Arquivo de Migração Inicial',
    'quarkus-flyway-create': 'Criar',
    'quarkus-flyway-update': 'Atualizar',
    'quarkus-flyway-create-dialog-description': 'Configure um arquivo inicial da geração de esquema do Hibernate ORM para funcionar com as migrações do Flyway.<br/>Se você disser sim, um arquivo inicial em <code>db/migrations</code> será <br/>criado e você poderá adicionar arquivos de migração adicionais conforme documentado.',
    'quarkus-flyway-update-dialog-description': 'Crie um arquivo de migração incremental a partir da diferença de esquema do Hibernate ORM.<br/>Se você disser sim, um arquivo adicional em <code>db/migrations</code> será <br/>criado.',
    'quarkus-flyway-cancel': 'Cancelar',
    'quarkus-flyway-clean-confirm': 'Isso irá eliminar todos os objetos (tabelas, visualizações, procedimentos, gatilhos, ...) no esquema configurado. Você deseja continuar?',
    'quarkus-flyway-datasource-title': str`Fonte de Dados ${0}`,
};

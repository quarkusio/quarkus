import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-flyway-meta-description':'管理您的数据库架构迁移',
    // Pages
    'quarkus-flyway-datasources':'数据源',
    // General
    'quarkus-flyway-name': '名称',
    'quarkus-flyway-action': '操作',
    'quarkus-flyway-clean': '清理',
    'quarkus-flyway-migrate': '迁移',
    'quarkus-flyway-clean-disabled-tooltip': '已通过 quarkus.flyway.clean-disabled=true 禁用 Flyway 清理功能',
    'quarkus-flyway-update-button-title': '创建更新迁移文件。请务必手动审查创建的文件，因为它可能导致数据丢失',
    'quarkus-flyway-generate-migration-file': '生成迁移文件',
    'quarkus-flyway-create-button-title': '设置 Flyway 迁移工作所需的基本文件。将在 db/migrations 中创建初始文件，然后您可以添加其他迁移文件',
    'quarkus-flyway-create-initial-migration-file': '创建初始迁移文件',
    'quarkus-flyway-create': '创建',
    'quarkus-flyway-update': '更新',
    'quarkus-flyway-datasource-title': str`${0} 数据源`,
    'quarkus-flyway-create-dialog-description': '从 Hibernate ORM 架构生成设置初始文件，以使 Flyway 迁移正常工作。<br/>如果您确认，将在 <code>db/migrations</code> 中创建初始文件，<br/>然后您可以按照文档添加其他迁移文件。',
    'quarkus-flyway-update-dialog-description': '从 Hibernate ORM 架构差异创建增量迁移文件。<br/>如果您确认，将在 <code>db/migrations</code> 中创建额外的文件。',
    'quarkus-flyway-cancel': '取消',
    'quarkus-flyway-clean-confirm': '这将删除配置架构中的所有对象（表、视图、存储过程、触发器等）。您要继续吗？'
};

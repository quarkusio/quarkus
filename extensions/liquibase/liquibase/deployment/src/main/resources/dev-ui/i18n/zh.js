import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-liquibase-meta-description':'使用 Liquibase 处理数据库架构迁移',
    // Pages
    'quarkus-liquibase-datasources':'数据源',
    // General
    'quarkus-liquibase-loading-datasources': '正在加载数据源...',
    'quarkus-liquibase-name': '名称',
    'quarkus-liquibase-action': '操作',
    'quarkus-liquibase-clear-database': '清除数据库',
    'quarkus-liquibase-clear': '清除',
    'quarkus-liquibase-clear-confirm': '这将删除配置架构中的所有对象（表、视图、过程、触发器...）。您要继续吗？',
    'quarkus-liquibase-cleared': str`数据源 ${0} 已被清除。`,
    'quarkus-liquibase-migrate': '迁移',
    'quarkus-liquibase-migrated': str`数据源 ${0} 已被迁移。`
};


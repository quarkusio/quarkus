import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-hibernate-search-orm-elasticsearch-meta-description':'在 Elasticsearch 中自动索引您的 Hibernate 实体',
    // Pages
    'quarkus-hibernate-search-orm-elasticsearch-indexed_entity_types':'已索引的实体类型',
    // General
    'quarkus-hibernate-search-orm-elasticsearch-loading': '加载中...',
    'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units':
        '未找到持久化单元。',
    'quarkus-hibernate-search-orm-elasticsearch-persistence-unit':
        '持久化单元',
    'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities':
        '未找到已索引的实体。',
    'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types':
        str`已选择 ${0} 个实体类型`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-selected':
        '重新索引已选择的',
    'quarkus-hibernate-search-orm-elasticsearch-entity-name':
        '实体名称',
    'quarkus-hibernate-search-orm-elasticsearch-class-name':
        '类名',
    'quarkus-hibernate-search-orm-elasticsearch-index-names':
        '索引名称',
    'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex':
        str`选择要为持久化单元 '${0}' 重新索引的实体类型。`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-started':
        str`已请求为持久化单元 '${1}' 重新索引 ${0} 个实体类型。`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-success':
        str`成功为持久化单元 '${1}' 重新索引了 ${0} 个实体类型。`,
    'quarkus-hibernate-search-orm-elasticsearch-reindex-error':
        str`为持久化单元 '${1}' 重新索引 ${0} 个实体类型时发生错误：\n${2}`
};

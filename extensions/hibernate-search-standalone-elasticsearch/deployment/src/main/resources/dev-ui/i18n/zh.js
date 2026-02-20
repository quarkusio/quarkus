import { str } from '@lit/localize';

export const templates = {
    // Metadata
    'quarkus-hibernate-search-standalone-elasticsearch-meta-description':'在 Elasticsearch 中显式索引/搜索实体',
    // Pages
    'quarkus-hibernate-search-standalone-elasticsearch-indexed_entity_types':'已索引的实体类型',
    // General
    'quarkus-hibernate-search-standalone-elasticsearch-loading': '加载中...',
    'quarkus-hibernate-search-standalone-elasticsearch-no-indexed-entities': '未找到已索引的实体。',
    'quarkus-hibernate-search-standalone-elasticsearch-selected-entity-types': str`已选择 ${0} 个实体类型${1}`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-selected': '重新索引已选择的',
    'quarkus-hibernate-search-standalone-elasticsearch-entity-name': '实体名称',
    'quarkus-hibernate-search-standalone-elasticsearch-class-name': '类名',
    'quarkus-hibernate-search-standalone-elasticsearch-index-names': '索引名称',
    'quarkus-hibernate-search-standalone-elasticsearch-select-entity-types': '选择要重新索引的实体类型。',
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-started': str`已请求重新索引 ${0} 个实体类型。`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-success': str`成功重新索引了 ${0} 个实体类型。`,
    'quarkus-hibernate-search-standalone-elasticsearch-reindex-error': str`重新索引 ${0} 个实体类型时发生错误：\n${1}`
};


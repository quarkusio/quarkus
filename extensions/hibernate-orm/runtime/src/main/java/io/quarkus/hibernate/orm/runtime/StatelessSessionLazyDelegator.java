package io.quarkus.hibernate.orm.runtime;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Plays the exact same role as {@link org.hibernate.engine.spi.SessionLazyDelegator} for {@link org.hibernate.Session}
 */
class StatelessSessionLazyDelegator implements StatelessSession {

    private final Supplier<StatelessSession> delegate;

    public StatelessSessionLazyDelegator(Supplier<StatelessSession> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        delegate.get().close();
    }

    @Override
    public void insert(Object entity) {
        delegate.get().insert(entity);
    }

    @Override
    public Object insert(String entityName, Object entity) {
        return delegate.get().insert(entityName, entity);
    }

    @Override
    public void insertMultiple(List<?> entities) {
        delegate.get().insertMultiple(entities);
    }

    @Override
    public void update(Object entity) {
        delegate.get().update(entity);
    }

    @Override
    public void update(String entityName, Object entity) {
        delegate.get().update(entityName, entity);
    }

    @Override
    public void updateMultiple(List<?> entities) {
        delegate.get().updateMultiple(entities);
    }

    @Override
    public void delete(Object entity) {
        delegate.get().delete(entity);
    }

    @Override
    public void delete(String entityName, Object entity) {
        delegate.get().delete(entityName, entity);
    }

    @Override
    public void deleteMultiple(List<?> entities) {
        delegate.get().deleteMultiple(entities);
    }

    @Override
    public Object get(String entityName, Object id) {
        return delegate.get().get(entityName, id);
    }

    @Override
    public Object get(String entityName, Object key, FindOption... findOptions) {
        return delegate.get().get(entityName, key, findOptions);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id) {
        return delegate.get().get(entityClass, id);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id, FindOption... options) {
        return delegate.get().get(entityClass, id, options);
    }

    @Override
    public <T> T get(EntityGraph<T> graph, Object id, FindOption... options) {
        return delegate.get().get(graph, id, options);
    }

    @Override
    public <T> List<T> getMultiple(Class<T> entityClass, List<?> ids, FindOption... options) {
        return delegate.get().getMultiple(entityClass, ids, options);
    }

    @Override
    public <T> List<T> getMultiple(EntityGraph<T> graph, List<?> ids, FindOption... options) {
        return delegate.get().getMultiple(graph, ids, options);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object id) {
        return delegate.get().find(entityClass, id);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object id, FindOption... options) {
        return delegate.get().find(entityClass, id, options);
    }

    @Override
    public <T> T find(EntityGraph<T> graph, Object id, FindOption... options) {
        return delegate.get().find(graph, id, options);
    }

    @Override
    public <T> List<T> findMultiple(Class<T> entityClass, List<?> ids, FindOption... options) {
        return delegate.get().findMultiple(entityClass, ids, options);
    }

    @Override
    public <T> List<T> findMultiple(EntityGraph<T> graph, List<?> ids, FindOption... options) {
        return delegate.get().findMultiple(graph, ids, options);
    }

    @Override
    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        delegate.get().setCacheRetrieveMode(cacheRetrieveMode);
    }

    @Override
    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        delegate.get().setCacheStoreMode(cacheStoreMode);
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return delegate.get().getCacheRetrieveMode();
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return delegate.get().getCacheStoreMode();
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        delegate.get().setProperty(propertyName, value);
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.get().getProperties();
    }

    @Override
    public Object get(String entityName, Object id, LockMode lockMode) {
        return delegate.get().get(entityName, id, lockMode);
    }

    @Override
    public <T> List<T> getMultiple(EntityGraph<T> entityGraph, GraphSemantic graphSemantic, List<?> ids) {
        return delegate.get().getMultiple(entityGraph, graphSemantic, ids);
    }

    @Override
    public Filter enableFilter(String filterName) {
        return delegate.get().enableFilter(filterName);
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        return delegate.get().getEnabledFilter(filterName);
    }

    @Override
    public void disableFilter(String filterName) {
        delegate.get().disableFilter(filterName);
    }

    @Override
    public void refresh(Object entity) {
        delegate.get().refresh(entity);
    }

    @Override
    public void refreshMultiple(List<?> entities) {
        delegate.get().refreshMultiple(entities);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        delegate.get().refresh(entity, lockMode);
    }

    @Override
    public void addOption(EntityAgent.Option option) {
        delegate.get().addOption(option);
    }

    @Override
    public Set<Option> getOptions() {
        return delegate.get().getOptions();
    }

    @Override
    public void refresh(String entityName, Object entity) {
        delegate.get().refresh(entityName, entity);
    }

    @Override
    public void refresh(Object entity, LockMode lockMode) {
        delegate.get().refresh(entity, lockMode);
    }

    @Override
    public void refresh(String entityName, Object entity, LockMode lockMode) {
        delegate.get().refresh(entityName, entity, lockMode);
    }

    @Override
    public <T> T fetch(T association) {
        return delegate.get().fetch(association);
    }

    @Override
    public Object getIdentifier(Object entity) {
        return delegate.get().getIdentifier(entity);
    }

    @Override
    public String getTenantIdentifier() {
        return delegate.get().getTenantIdentifier();
    }

    @Override
    public Object getTenantIdentifierValue() {
        return delegate.get().getTenantIdentifierValue();
    }

    @Override
    public boolean isOpen() {
        return delegate.get().isOpen();
    }

    @Override
    public boolean isConnected() {
        return delegate.get().isConnected();
    }

    @Override
    public Transaction beginTransaction() {
        return delegate.get().beginTransaction();
    }

    @Override
    public Transaction getTransaction() {
        return delegate.get().getTransaction();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return delegate.get().getEntityManagerFactory();
    }

    @Override
    public void joinTransaction() {
        delegate.get().joinTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return delegate.get().isJoinedToTransaction();
    }

    @Override
    public Object find(String entityName, Object key, FindOption... findOptions) {
        return delegate.get().find(entityName, key, findOptions);
    }

    @Override
    public ProcedureCall getNamedProcedureCall(String name) {
        return delegate.get().getNamedProcedureCall(name);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName) {
        return delegate.get().createStoredProcedureCall(procedureName);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
        return delegate.get().createStoredProcedureCall(procedureName, resultClasses);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
        return delegate.get().createStoredProcedureCall(procedureName, resultSetMappings);
    }

    @Override
    public ProcedureCall createNamedStoredProcedureQuery(String name) {
        return delegate.get().createNamedStoredProcedureQuery(name);
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName) {
        return delegate.get().createStoredProcedureQuery(procedureName);
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
        return delegate.get().createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return delegate.get().createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public Integer getJdbcBatchSize() {
        return delegate.get().getJdbcBatchSize();
    }

    @Override
    public void setJdbcBatchSize(Integer jdbcBatchSize) {
        delegate.get().setJdbcBatchSize(jdbcBatchSize);
    }

    @Override
    public HibernateCriteriaBuilder getCriteriaBuilder() {
        return delegate.get().getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return delegate.get().getMetamodel();
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        delegate.get().doWork(work);
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
        return delegate.get().doReturningWork(work);
    }

    @Override
    public MutationOrSelectionQuery createQuery(String queryString) {
        return delegate.get().createQuery(queryString);
    }

    @Override
    public <R> SelectionQuery<R> createQuery(String queryString, Class<R> resultClass) {
        return delegate.get().createQuery(queryString, resultClass);
    }

    @Override
    public <T> SelectionQuery<T> createQuery(String hqlString, EntityGraph<T> entityGraph) {
        return delegate.get().createQuery(hqlString, entityGraph);
    }

    @Override
    public <R> SelectionQuery<R> createQuery(TypedQueryReference<R> typedQueryReference) {
        return delegate.get().createQuery(typedQueryReference);
    }

    @Override
    public <T> SelectionQuery<T> createQuery(CriteriaSelect<T> criteriaSelect) {
        return delegate.get().createQuery(criteriaSelect);
    }

    @Override
    public MutationQuery createStatement(CriteriaStatement<?> criteriaStatement) {
        return delegate.get().createStatement(criteriaStatement);
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(CriteriaSelect<R> criteria) {
        return delegate.get().createSelectionQuery(criteria);
    }

    @Override
    public NativeQuery<?> createNativeQuery(String sqlString) {
        return delegate.get().createNativeQuery(sqlString);
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass) {
        return delegate.get().createNativeQuery(sqlString, resultClass);
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
        return delegate.get().createNativeQuery(sqlString, resultClass, tableAlias);
    }

    @Override
    public NativeQuery<?> createNativeQuery(String sqlString, String resultSetMappingName) {
        return delegate.get().createNativeQuery(sqlString, resultSetMappingName);
    }

    @Override
    public <T> TypedQuery<T> createNativeQuery(String sql, ResultSetMapping<T> resultSetMapping) {
        return delegate.get().createNativeQuery(sql, resultSetMapping);
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
        return delegate.get().createNativeQuery(sqlString, resultSetMappingName, resultClass);
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
        return delegate.get().createSelectionQuery(hqlString, resultType);
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
        return delegate.get().createSelectionQuery(criteria);
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph) {
        return delegate.get().createSelectionQuery(hqlString, resultGraph);
    }

    @Override
    public MutationQuery createMutationQuery(String hqlString) {
        return delegate.get().createMutationQuery(hqlString);
    }

    @Override
    public MutationQuery createStatement(String hqlString) {
        return delegate.get().createStatement(hqlString);
    }

    @Override
    public MutationQuery createStatement(StatementReference statementReference) {
        return delegate.get().createStatement(statementReference);
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaStatement<?> criteriaStatement) {
        return delegate.get().createMutationQuery(criteriaStatement);
    }

    @Override
    public MutationQuery createMutationQuery(JpaCriteriaInsert insert) {
        return delegate.get().createMutationQuery(insert);
    }

    @Override
    public MutationQuery createNativeMutationQuery(String sqlString) {
        return delegate.get().createNativeMutationQuery(sqlString);
    }

    @Override
    public MutationOrSelectionQuery createNamedQuery(String name) {
        return delegate.get().createNamedQuery(name);
    }

    @Override
    public <R> SelectionQuery<R> createNamedQuery(String name, Class<R> resultClass) {
        return delegate.get().createNamedQuery(name, resultClass);
    }

    @Override
    public MutationQuery createNamedStatement(String name) {
        return delegate.get().createNamedStatement(name);
    }

    @Override
    public <R> NativeQuery<R> createNamedQuery(String name, String resultSetMappingName) {
        return delegate.get().createNamedQuery(name, resultSetMappingName);
    }

    @Override
    public <R> NativeQuery<R> createNamedQuery(String name, String resultSetMappingName, Class<R> resultClass) {
        return delegate.get().createNamedQuery(name, resultSetMappingName, resultClass);
    }

    @Override
    public MutationQuery createNativeStatement(String sql) {
        return delegate.get().createNativeStatement(sql);
    }

    @Override
    public <R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType) {
        return delegate.get().createNamedSelectionQuery(name, resultType);
    }

    @Override
    public MutationQuery createNamedMutationQuery(String name) {
        return delegate.get().createNamedMutationQuery(name);
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
        return delegate.get().createEntityGraph(rootType);
    }

    @Override
    public RootGraph<?> createEntityGraph(String graphName) {
        return delegate.get().createEntityGraph(graphName);
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
        return delegate.get().createEntityGraph(rootType, graphName);
    }

    @Override
    public RootGraph<?> getEntityGraph(String graphName) {
        return delegate.get().getEntityGraph(graphName);
    }

    @Override
    public <T> RootGraph<T> getEntityGraph(Class<T> rootType, String graphName) {
        return delegate.get().getEntityGraph(rootType, graphName);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return delegate.get().getEntityGraphs(entityClass);
    }

    @Override
    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        delegate.get().runWithConnection(action);
    }

    @Override
    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        return delegate.get().callWithConnection(function);
    }

    @Override
    public SharedSessionBuilder sessionWithOptions() {
        return delegate.get().sessionWithOptions();
    }

    @Override
    public SharedStatelessSessionBuilder statelessWithOptions() {
        return delegate.get().statelessWithOptions();
    }

    @Override
    public void inTransaction(Consumer<? super Transaction> action) {
        delegate.get().inTransaction(action);
    }

    @Override
    public <R> R fromTransaction(Function<? super Transaction, R> action) {
        return delegate.get().fromTransaction(action);
    }

    @Override
    public SessionFactory getFactory() {
        return delegate.get().getFactory();
    }

    @Override
    public void upsert(Object entity) {
        delegate.get().upsert(entity);
    }

    @Override
    public void upsert(String entityName, Object entity) {
        delegate.get().upsert(entityName, entity);
    }

    @Override
    public void upsertMultiple(List<?> entities) {
        delegate.get().upsertMultiple(entities);
    }

    @Override
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id) {
        return delegate.get().get(graph, graphSemantic, id);
    }

    @Override
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id, LockMode lockMode) {
        return delegate.get().get(graph, graphSemantic, id, lockMode);
    }

    @Override
    public CacheMode getCacheMode() {
        return delegate.get().getCacheMode();
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        delegate.get().setCacheMode(cacheMode);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return delegate.get().unwrap(type);
    }
}

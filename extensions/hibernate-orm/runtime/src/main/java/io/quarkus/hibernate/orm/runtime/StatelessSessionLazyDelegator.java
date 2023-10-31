package io.quarkus.hibernate.orm.runtime;

import java.util.List;
import java.util.function.Supplier;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;

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
    public Object insert(Object entity) {
        return delegate.get().insert(entity);
    }

    @Override
    public Object insert(String entityName, Object entity) {
        return delegate.get().insert(entityName, entity);
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
    public void delete(Object entity) {
        delegate.get().delete(entity);
    }

    @Override
    public void delete(String entityName, Object entity) {
        delegate.get().delete(entityName, entity);
    }

    @Override
    public Object get(String entityName, Object id) {
        return delegate.get().get(entityName, id);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id) {
        return delegate.get().get(entityClass, id);
    }

    @Override
    public Object get(String entityName, Object id, LockMode lockMode) {
        return delegate.get().get(entityName, id, lockMode);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) {
        return delegate.get().get(entityClass, id, lockMode);
    }

    @Override
    public void refresh(Object entity) {
        delegate.get().refresh(entity);
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
    public void fetch(Object association) {
        delegate.get().fetch(association);
    }

    @Override
    public String getTenantIdentifier() {
        return delegate.get().getTenantIdentifier();
    }

    @Override
    public Object getTenantIdentifierValue() {
        return delegate.get().getTenantIdentifier();
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
    public void joinTransaction() {
        delegate.get().joinTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return delegate.get().isJoinedToTransaction();
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
    public void doWork(Work work) throws HibernateException {
        delegate.get().doWork(work);
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
        return delegate.get().doReturningWork(work);
    }

    @Override
    @Deprecated(since = "6.0")
    public Query createQuery(String queryString) {
        return delegate.get().createQuery(queryString);
    }

    @Override
    public <R> Query<R> createQuery(String queryString, Class<R> resultClass) {
        return delegate.get().createQuery(queryString, resultClass);
    }

    @Override
    public <R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery) {
        return delegate.get().createQuery(criteriaQuery);
    }

    @Override
    @Deprecated(since = "6.0")
    public Query createQuery(CriteriaUpdate updateQuery) {
        return delegate.get().createQuery(updateQuery);
    }

    @Override
    @Deprecated(since = "6.0")
    public Query createQuery(CriteriaDelete deleteQuery) {
        return delegate.get().createQuery(deleteQuery);
    }

    @Override
    @Deprecated(since = "6.0")
    public NativeQuery createNativeQuery(String sqlString) {
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
    @Deprecated(since = "6.0")
    public NativeQuery createNativeQuery(String sqlString, String resultSetMappingName) {
        return delegate.get().createNativeQuery(sqlString, resultSetMappingName);
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
        return delegate.get().createNativeQuery(sqlString, resultSetMappingName, resultClass);
    }

    @Override
    public SelectionQuery<?> createSelectionQuery(String hqlString) {
        return delegate.get().createSelectionQuery(hqlString);
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
    public MutationQuery createMutationQuery(String hqlString) {
        return delegate.get().createMutationQuery(hqlString);
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaUpdate updateQuery) {
        return delegate.get().createMutationQuery(updateQuery);
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaDelete deleteQuery) {
        return delegate.get().createMutationQuery(deleteQuery);
    }

    @Override
    public MutationQuery createMutationQuery(JpaCriteriaInsertSelect insertSelect) {
        return delegate.get().createMutationQuery(insertSelect);
    }

    @Override
    public MutationQuery createNativeMutationQuery(String sqlString) {
        return delegate.get().createNativeMutationQuery(sqlString);
    }

    @Override
    @Deprecated(since = "6.0")
    public Query createNamedQuery(String name) {
        return delegate.get().createNamedQuery(name);
    }

    @Override
    public <R> Query<R> createNamedQuery(String name, Class<R> resultClass) {
        return delegate.get().createNamedQuery(name, resultClass);
    }

    @Override
    public SelectionQuery<?> createNamedSelectionQuery(String name) {
        return delegate.get().createNamedSelectionQuery(name);
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
    @Deprecated(since = "6.0")
    public Query getNamedQuery(String queryName) {
        return delegate.get().getNamedQuery(queryName);
    }

    @Override
    @Deprecated(since = "6.0")
    public NativeQuery getNamedNativeQuery(String name) {
        return delegate.get().getNamedNativeQuery(name);
    }

    @Override
    @Deprecated(since = "6.0")
    public NativeQuery getNamedNativeQuery(String name, String resultSetMapping) {
        return delegate.get().getNamedNativeQuery(name, resultSetMapping);
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
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return delegate.get().getEntityGraphs(entityClass);
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
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id) {
        return delegate.get().get(graph, graphSemantic, id);
    }

    @Override
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id, LockMode lockMode) {
        return delegate.get().get(graph, graphSemantic, id, lockMode);
    }
}

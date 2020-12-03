package io.quarkus.hibernate.orm.runtime.session;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.stat.SessionStatistics;

public abstract class ForwardingSession implements Session {

    protected abstract Session delegate();

    @Override
    public void persist(Object entity) {
        delegate().persist(entity);
    }

    @Override
    public Object merge(Object entity) {
        return delegate().merge(entity);
    }

    @Override
    public void remove(Object entity) {
        delegate().remove(entity);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        return delegate().find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        return delegate().find(entityClass, primaryKey, properties);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        return delegate().find(entityClass, primaryKey, lockMode);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        return delegate().find(entityClass, primaryKey, lockMode, properties);
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return delegate().getReference(entityClass, primaryKey);
    }

    @Override
    public void flush() {
        delegate().flush();
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        delegate().setFlushMode(flushMode);
    }

    @Override
    public FlushModeType getFlushMode() {
        return delegate().getFlushMode();
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        delegate().lock(entity, lockMode);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        delegate().lock(entity, lockMode, properties);
    }

    @Override
    public void refresh(Object entity) {
        delegate().refresh(entity);
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        delegate().refresh(entity, properties);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        delegate().refresh(entity, lockMode);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        delegate().refresh(entity, lockMode, properties);
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public void detach(Object entity) {
        delegate().detach(entity);
    }

    @Override
    public boolean contains(Object entity) {
        return delegate().contains(entity);
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        return delegate().getLockMode(entity);
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        delegate().setProperty(propertyName, value);
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate().getProperties();
    }

    @Override
    public Query createQuery(String qlString) {
        return delegate().createQuery(qlString);
    }

    @Override
    public <T> Query<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        return delegate().createQuery(criteriaQuery);
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        return delegate().createQuery(updateQuery);
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        return delegate().createQuery(deleteQuery);
    }

    @Override
    public <T> Query<T> createQuery(String qlString, Class<T> resultClass) {
        return delegate().createQuery(qlString, resultClass);
    }

    @Override
    public Query createNamedQuery(String name) {
        return delegate().createNamedQuery(name);
    }

    @Override
    public <T> Query<T> createNamedQuery(String name, Class<T> resultClass) {
        return delegate().createNamedQuery(name, resultClass);
    }

    @Override
    public NativeQuery createNativeQuery(String sqlString) {
        return delegate().createNativeQuery(sqlString);
    }

    @Override
    public NativeQuery createNativeQuery(String sqlString, Class resultClass) {
        return delegate().createNativeQuery(sqlString, resultClass);
    }

    @Override
    public NativeQuery createNativeQuery(String sqlString, String resultSetMapping) {
        return delegate().createNativeQuery(sqlString, resultSetMapping);
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        return delegate().createNamedStoredProcedureQuery(name);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        return delegate().createStoredProcedureQuery(procedureName);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        return delegate().createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return delegate().createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public void joinTransaction() {
        delegate().joinTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return delegate().isJoinedToTransaction();
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return delegate().unwrap(cls);
    }

    @Override
    public Object getDelegate() {
        return delegate().getDelegate();
    }

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public boolean isOpen() {
        return delegate().isOpen();
    }

    @Override
    public Transaction getTransaction() {
        return delegate().getTransaction();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return delegate().getEntityManagerFactory();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return delegate().getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return delegate().getMetamodel();
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
        return delegate().createEntityGraph(rootType);
    }

    @Override
    public RootGraph<?> createEntityGraph(String graphName) {
        return delegate().createEntityGraph(graphName);
    }

    @Override
    public RootGraph<?> getEntityGraph(String graphName) {
        return delegate().getEntityGraph(graphName);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return delegate().getEntityGraphs(entityClass);
    }

    @Override
    public SharedSessionBuilder sessionWithOptions() {
        return delegate().sessionWithOptions();
    }

    @Override
    @Deprecated
    public void setFlushMode(FlushMode flushMode) {
        delegate().setFlushMode(flushMode);
    }

    @Override
    public void setHibernateFlushMode(FlushMode flushMode) {
        delegate().setHibernateFlushMode(flushMode);
    }

    @Override
    public FlushMode getHibernateFlushMode() {
        return delegate().getHibernateFlushMode();
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        delegate().setCacheMode(cacheMode);
    }

    @Override
    public CacheMode getCacheMode() {
        return delegate().getCacheMode();
    }

    @Override
    public SessionFactory getSessionFactory() {
        return delegate().getSessionFactory();
    }

    @Override
    public void cancelQuery() throws HibernateException {
        delegate().cancelQuery();
    }

    @Override
    public boolean isDirty() throws HibernateException {
        return delegate().isDirty();
    }

    @Override
    public boolean isDefaultReadOnly() {
        return delegate().isDefaultReadOnly();
    }

    @Override
    public void setDefaultReadOnly(boolean readOnly) {
        delegate().setDefaultReadOnly(readOnly);
    }

    @Override
    public Serializable getIdentifier(Object object) {
        return delegate().getIdentifier(object);
    }

    @Override
    public boolean contains(String entityName, Object object) {
        return delegate().contains(entityName, object);
    }

    @Override
    public void evict(Object object) {
        delegate().evict(object);
    }

    @Override
    public <T> T load(Class<T> theClass, Serializable id, LockMode lockMode) {
        return delegate().load(theClass, id, lockMode);
    }

    @Override
    public <T> T load(Class<T> theClass, Serializable id, LockOptions lockOptions) {
        return delegate().load(theClass, id, lockOptions);
    }

    @Override
    public Object load(String entityName, Serializable id, LockMode lockMode) {
        return delegate().load(entityName, id, lockMode);
    }

    @Override
    public Object load(String entityName, Serializable id, LockOptions lockOptions) {
        return delegate().load(entityName, id, lockOptions);
    }

    @Override
    public <T> T load(Class<T> theClass, Serializable id) {
        return delegate().load(theClass, id);
    }

    @Override
    public Object load(String entityName, Serializable id) {
        return delegate().load(entityName, id);
    }

    @Override
    public void load(Object object, Serializable id) {
        delegate().load(object, id);
    }

    @Override
    public void replicate(Object object, ReplicationMode replicationMode) {
        delegate().replicate(object, replicationMode);
    }

    @Override
    public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
        delegate().replicate(entityName, object, replicationMode);
    }

    @Override
    public Serializable save(Object object) {
        return delegate().save(object);
    }

    @Override
    public Serializable save(String entityName, Object object) {
        return delegate().save(entityName, object);
    }

    @Override
    public void saveOrUpdate(Object object) {
        delegate().saveOrUpdate(object);
    }

    @Override
    public void saveOrUpdate(String entityName, Object object) {
        delegate().saveOrUpdate(entityName, object);
    }

    @Override
    public void update(Object object) {
        delegate().update(object);
    }

    @Override
    public void update(String entityName, Object object) {
        delegate().update(entityName, object);
    }

    @Override
    public Object merge(String entityName, Object object) {
        return delegate().merge(entityName, object);
    }

    @Override
    public void persist(String entityName, Object object) {
        delegate().persist(entityName, object);
    }

    @Override
    public void delete(Object object) {
        delegate().delete(object);
    }

    @Override
    public void delete(String entityName, Object object) {
        delegate().delete(entityName, object);
    }

    @Override
    public void lock(Object object, LockMode lockMode) {
        delegate().lock(object, lockMode);
    }

    @Override
    public void lock(String entityName, Object object, LockMode lockMode) {
        delegate().lock(entityName, object, lockMode);
    }

    @Override
    public LockRequest buildLockRequest(LockOptions lockOptions) {
        return delegate().buildLockRequest(lockOptions);
    }

    @Override
    public void refresh(String entityName, Object object) {
        delegate().refresh(entityName, object);
    }

    @Override
    public void refresh(Object object, LockMode lockMode) {
        delegate().refresh(object, lockMode);
    }

    @Override
    public void refresh(Object object, LockOptions lockOptions) {
        delegate().refresh(object, lockOptions);
    }

    @Override
    public void refresh(String entityName, Object object, LockOptions lockOptions) {
        delegate().refresh(entityName, object, lockOptions);
    }

    @Override
    public LockMode getCurrentLockMode(Object object) {
        return delegate().getCurrentLockMode(object);
    }

    @Override
    @Deprecated
    public org.hibernate.Query createFilter(Object collection, String queryString) {
        return delegate().createFilter(collection, queryString);
    }

    @Override
    public <T> T get(Class<T> entityType, Serializable id) {
        return delegate().get(entityType, id);
    }

    @Override
    public <T> T get(Class<T> entityType, Serializable id, LockMode lockMode) {
        return delegate().get(entityType, id, lockMode);
    }

    @Override
    public <T> T get(Class<T> entityType, Serializable id, LockOptions lockOptions) {
        return delegate().get(entityType, id, lockOptions);
    }

    @Override
    public Object get(String entityName, Serializable id) {
        return delegate().get(entityName, id);
    }

    @Override
    public Object get(String entityName, Serializable id, LockMode lockMode) {
        return delegate().get(entityName, id, lockMode);
    }

    @Override
    public Object get(String entityName, Serializable id, LockOptions lockOptions) {
        return delegate().get(entityName, id, lockOptions);
    }

    @Override
    public String getEntityName(Object object) {
        return delegate().getEntityName(object);
    }

    @Override
    public IdentifierLoadAccess byId(String entityName) {
        return delegate().byId(entityName);
    }

    @Override
    public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
        return delegate().byMultipleIds(entityClass);
    }

    @Override
    public MultiIdentifierLoadAccess byMultipleIds(String entityName) {
        return delegate().byMultipleIds(entityName);
    }

    @Override
    public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
        return delegate().byId(entityClass);
    }

    @Override
    public NaturalIdLoadAccess byNaturalId(String entityName) {
        return delegate().byNaturalId(entityName);
    }

    @Override
    public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
        return delegate().byNaturalId(entityClass);
    }

    @Override
    public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
        return delegate().bySimpleNaturalId(entityName);
    }

    @Override
    public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
        return delegate().bySimpleNaturalId(entityClass);
    }

    @Override
    public Filter enableFilter(String filterName) {
        return delegate().enableFilter(filterName);
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        return delegate().getEnabledFilter(filterName);
    }

    @Override
    public void disableFilter(String filterName) {
        delegate().disableFilter(filterName);
    }

    @Override
    public SessionStatistics getStatistics() {
        return delegate().getStatistics();
    }

    @Override
    public boolean isReadOnly(Object entityOrProxy) {
        return delegate().isReadOnly(entityOrProxy);
    }

    @Override
    public void setReadOnly(Object entityOrProxy, boolean readOnly) {
        delegate().setReadOnly(entityOrProxy, readOnly);
    }

    @Override
    public Connection disconnect() {
        return delegate().disconnect();
    }

    @Override
    public void reconnect(Connection connection) {
        delegate().reconnect(connection);
    }

    @Override
    public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
        return delegate().isFetchProfileEnabled(name);
    }

    @Override
    public void enableFetchProfile(String name) throws UnknownProfileException {
        delegate().enableFetchProfile(name);
    }

    @Override
    public void disableFetchProfile(String name) throws UnknownProfileException {
        delegate().disableFetchProfile(name);
    }

    @Override
    public TypeHelper getTypeHelper() {
        return delegate().getTypeHelper();
    }

    @Override
    public LobHelper getLobHelper() {
        return delegate().getLobHelper();
    }

    @Override
    public void addEventListeners(SessionEventListener... listeners) {
        delegate().addEventListeners(listeners);
    }

    @Override
    public NativeQuery createSQLQuery(String queryString) {
        return delegate().createSQLQuery(queryString);
    }

    @Override
    public String getTenantIdentifier() {
        return delegate().getTenantIdentifier();
    }

    @Override
    public boolean isConnected() {
        return delegate().isConnected();
    }

    @Override
    public Transaction beginTransaction() {
        return delegate().beginTransaction();
    }

    @Override
    public Query getNamedQuery(String queryName) {
        return delegate().getNamedQuery(queryName);
    }

    @Override
    public ProcedureCall getNamedProcedureCall(String name) {
        return delegate().getNamedProcedureCall(name);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName) {
        return delegate().createStoredProcedureCall(procedureName);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
        return delegate().createStoredProcedureCall(procedureName, resultClasses);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
        return delegate().createStoredProcedureCall(procedureName, resultSetMappings);
    }

    @Override
    @Deprecated
    public Criteria createCriteria(Class persistentClass) {
        return delegate().createCriteria(persistentClass);
    }

    @Override
    @Deprecated
    public Criteria createCriteria(Class persistentClass, String alias) {
        return delegate().createCriteria(persistentClass, alias);
    }

    @Override
    @Deprecated
    public Criteria createCriteria(String entityName) {
        return delegate().createCriteria(entityName);
    }

    @Override
    @Deprecated
    public Criteria createCriteria(String entityName, String alias) {
        return delegate().createCriteria(entityName, alias);
    }

    @Override
    public Integer getJdbcBatchSize() {
        return delegate().getJdbcBatchSize();
    }

    @Override
    public void setJdbcBatchSize(Integer jdbcBatchSize) {
        delegate().setJdbcBatchSize(jdbcBatchSize);
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        delegate().doWork(work);
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
        return delegate().doReturningWork(work);
    }

    @Override
    @Deprecated
    public org.hibernate.Query getNamedSQLQuery(String name) {
        return delegate().getNamedSQLQuery(name);
    }

    @Override
    public NativeQuery getNamedNativeQuery(String name) {
        return delegate().getNamedNativeQuery(name);
    }

    @Override
    public Session getSession() {
        return delegate().getSession();
    }
}

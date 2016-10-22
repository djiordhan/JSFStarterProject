package com.djiordhan.starter.data.dao;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;

public abstract class AbstractDAO<T> {

    protected EntityManagerFactory factory;
    protected UserTransaction utx;

    public void construct(EntityManagerFactory factory, UserTransaction utx) {
        this.factory = factory;
        this.utx = utx;
    }

    public abstract Class<T> getEntityClass();

    public T findById(int id) {
        EntityManager em = factory.createEntityManager();
        T entity = em.find(getEntityClass(), id);
        em.close();
        return entity;
    }

    public T refresh(T entity) {
        EntityManager em = factory.createEntityManager();
        entity = em.merge(entity);
        em.refresh(entity);
        em.close();
        return entity;
    }

    public List<T> findAll() {
        EntityManager em = factory.createEntityManager();
        List<T> results = em.createQuery("SELECT o FROM " + getEntityClass().getSimpleName() + " o", getEntityClass()).getResultList();
        em.close();
        return results;
    }

    public List<T> query(String query) {
        EntityManager em = factory.createEntityManager();
        List<T> results = em.createQuery(query, getEntityClass()).getResultList();
        em.close();
        return results;
    }

    public T querySingle(String query) {
        EntityManager em = factory.createEntityManager();
        List<T> results = em.createQuery(query, getEntityClass()).getResultList();
        em.close();
        if (!results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }
    
    public List<T> query(String query, int maxResults)  {
         EntityManager em = factory.createEntityManager();
        TypedQuery<T> typedQuery = em.createQuery(query, getEntityClass());
        typedQuery.setMaxResults(maxResults);
        List<T> results = typedQuery.getResultList();
        em.close();
        return results;
    }

    public List<T> query(String query, Map<String, Object> params) {
        EntityManager em = factory.createEntityManager();
        TypedQuery<T> typedQuery = em.createQuery(query, getEntityClass());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            typedQuery.setParameter(entry.getKey(), entry.getValue());
        }
        List<T> results = typedQuery.getResultList();
        em.close();
        return results;
    }

    public T persist(T entity) {
        return (T)executeTransaction(new PersistTransaction(entity));
    }

    public T merge(T entity) {
        return (T)executeTransaction(new MergeTransaction(entity));
    }

    public void remove(T entity) {
        executeTransaction(new RemoveTransaction(entity));
    }

    private T executeTransaction(Transaction<T> transaction) {
        EntityManager em = factory.createEntityManager();
        UserTransaction userTransaction = utx;

        T entity = null;

        try {
            userTransaction.begin();
            entity = transaction.run(em);
            userTransaction.commit();
        } catch (Exception ex) {
            try {
                userTransaction.rollback();
            } catch (Exception ex1) {
                Logger.getLogger(AbstractDAO.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Logger.getLogger(AbstractDAO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            em.close();
            return entity;
        }
    }

    private class PersistTransaction implements Transaction {

        private T entity;

        public PersistTransaction(T entity) {
            this.entity = entity;
        }

        @Override
        public T run(EntityManager em) {
            em.persist(entity);
            return entity;
        }
    }

    private class MergeTransaction implements Transaction {

        private T entity;

        public MergeTransaction(T entity) {
            this.entity = entity;
        }

        @Override
        public T run(EntityManager em) {
            return em.merge(entity);
        }
    }

    private class RemoveTransaction implements Transaction<T> {

        private T entity;

        public RemoveTransaction(T entity) {
            this.entity = entity;
        }

        @Override
        public T run(EntityManager em) {
            entity = em.merge(entity);
            em.remove(entity);
            return entity;
         }
    }

    private interface Transaction<T> {

        T run(EntityManager em);
    }
}

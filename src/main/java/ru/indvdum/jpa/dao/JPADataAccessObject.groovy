package ru.indvdum.jpa.dao
import java.sql.Connection
import java.sql.SQLException
import java.util.Map.Entry

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.NoResultException
import javax.persistence.Persistence
import javax.persistence.Query
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import javax.sql.DataSource

import org.apache.commons.configuration.XMLConfiguration
import org.apache.openjpa.conf.OpenJPAConfiguration
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI
import org.apache.openjpa.persistence.OpenJPAPersistence
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ru.indvdum.jpa.entities.AbstractEntity
import ru.indvdum.jpa.props.Props

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 08.11.2012 23:35:04
 *
 */
public class JPADataAccessObject {

	protected static Logger log = LoggerFactory.getLogger(JPADataAccessObject.class.getSimpleName());
	protected static String persistenceUnitName = null;
	protected static EntityManagerFactory emf = null;
	protected EntityManager em = getEMF().createEntityManager();
	protected EntityTransaction tx = null;

	JPADataAccessObject() {
		if (isTransactional()) {
			tx = em.getTransaction();
			tx.begin();
		}
	}

	protected boolean isTransactional() {
		return true;
	}

	void close() {
		commit()
		em.close()
		em = null
	}

	void commit() {
		if(tx != null && tx.isActive()) {
			tx.commit()
		}
	}

	void rollback() {
		if(tx != null && tx.isActive()) {
			tx.rollback()
		}
	}

	void detach(Object entity) {
		em.detach(entity);
	}

	void flush() {
		em.flush();
	}

	protected static EntityManagerFactory getEMF() {
		initEntityManagerFactory();
		return emf;
	}

	public static void initEntityManagerFactory() {
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory(getPersistenceUnitName(), JPAPropertySelector.select());
		}
	}

	protected static String getPersistenceUnitName() {
		if (persistenceUnitName != null)
			return persistenceUnitName;

		if (persistenceUnitName == null)
			persistenceUnitName = System.getProperty(Props.PERSISTANCE_UNIT_NAME_PROPERTY);
		if (persistenceUnitName == null) {
			try {
				persistenceUnitName = ResourceBundle.getBundle(Props.JPADAO_PROPERTY_FILE).getString(Props.PERSISTANCE_UNIT_NAME_PROPERTY);
			} catch (MissingResourceException e) {
				log.info("Configuration file " + Props.JPADAO_PROPERTY_FILE + ".properties not found");
			}
		}
		if (persistenceUnitName == null) {
			XMLConfiguration conf = new XMLConfiguration("META-INF/persistence.xml");
			persistenceUnitName = conf.getString("persistence-unit[@name]");
		}
		if (persistenceUnitName == null)
			persistenceUnitName = "database";
		return persistenceUnitName;
	}

	public static Connection getSQLConnection() throws SQLException {
		OpenJPAEntityManagerFactorySPI openjpaemf = (OpenJPAEntityManagerFactorySPI) OpenJPAPersistence.cast(getEMF());
		OpenJPAConfiguration conf = openjpaemf.getConfiguration();
		DataSource ds = (DataSource) conf.getConnectionFactory();
		return ds.getConnection();
	}

	public <T> T mergeAndRefresh(T object) {
		def merged = em.merge(object)
		em.refresh(merged)

		merged
	}

	public <T extends AbstractEntity> T read(T object) {
		return find(object.class, object.getIdentifierValue())
	}

	public <T> T merge(T object) {
		em.merge(object)
	}

	void refresh(Object ... refreshObjects) {
		for(object in refreshObjects) {
			em.refresh(object)
		}
	}

	boolean persist(Object ... persistanceQueue) {
		persistAndRemove(persistanceQueue, new Object[0])
	}

	boolean persist(Collection entities) {
		persistAndRemove(entities.toArray(), new Object[0])
	}

	boolean remove(Object ... removeQueue) {
		persistAndRemove(new Object[0], removeQueue)
	}

	boolean remove(Collection entities) {
		persistAndRemove(new Object[0], entities.toArray())
	}

	synchronized boolean persistAndRemove(Object[] persistanceQueue, Object[] removeQueue) {

		try {

			if(persistanceQueue != null) {
				for(object in persistanceQueue) {
					if(object != null) {
						em.persist(object)
					}
				}
			}

			if(removeQueue != null) {
				for(object in removeQueue) {
					if(object != null) {
						em.remove(object)
					}
				}
			}
		}
		catch(Throwable t) {
			log.error("Error while synchronizing with Database: ", t);
			return false;
		}

		return true
	}

	public <T> List<T> list(Class<T> entityClass) {
		// TODO: check for AbstractEntity type of T
		CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(entityClass);
		query.from(entityClass);
		return new ArrayList(em.createQuery(query).getResultList());
	}

	public <T> List<T> list(Class<T> entityClass, Map<String, Object> equalProperties, Map<String, Object> notEqualProperties) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> query = cb.createQuery(entityClass);
		Root<T> root = query.from(entityClass);
		Collection<Predicate> predicates = new HashSet<Predicate>();
		if(equalProperties != null) {
			for(Entry<String, Object> entry: equalProperties.entrySet()){
				String property = entry.getKey();
				Object value = entry.getValue();
				predicates.add(cb.equal(root.get(property), value));
			}
		}
		if(notEqualProperties != null) {
			for(Entry<String, Object> entry: equalProperties.entrySet()){
				String property = entry.getKey();
				Object value = entry.getValue();
				predicates.add(cb.notEqual(root.get(property), value));
			}
		}
		query.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
		return em.createQuery(query).getResultList();
	}

	public <T> List<T> list(Class<T> entityClass, Object ... equalFieldNamesAndValues) {
		if (equalFieldNamesAndValues.length % 2 != 0)
			throw new RuntimeException("Illegal arguments count: ${equalFieldNamesAndValues.length}");
		Map fieldValues = [:];
		for (int i = 0; i < equalFieldNamesAndValues.length;) {
			String field = (String) equalFieldNamesAndValues[i++];
			Object value = equalFieldNamesAndValues[i++];
			fieldValues["${field}"] = value;
		}
		return list(entityClass, fieldValues, null);
	}

	public List list(String jpql, Object ... paramValues) {
		Query query = em.createQuery(jpql);
		for (int i = 0; i < paramValues.length; i++)
			query.setParameter(i + 1, paramValues[i]);
		return query.getResultList();
	}

	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return em.find(entityClass, primaryKey);
	}

	public <T> T find(Class<T> entityClass, Map<String, Object> equalProperties, Map<String, Object> notEqualProperties) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> query = cb.createQuery(entityClass);
		Root<T> root = query.from(entityClass);
		Collection<Predicate> predicates = new HashSet<Predicate>();
		if(equalProperties != null) {
			for(Entry<String, Object> entry: equalProperties.entrySet()){
				String property = entry.getKey();
				Object value = entry.getValue();
				predicates.add(cb.equal(root.get(property), value));
			}
		}
		if(notEqualProperties != null) {
			for(Entry<String, Object> entry: equalProperties.entrySet()){
				String property = entry.getKey();
				Object value = entry.getValue();
				predicates.add(cb.notEqual(root.get(property), value));
			}
		}
		query.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
		T result = null;
		try {
			result = getSingleResult(em.createQuery(query));
		} catch (NoResultException e) {
			log.info("Object not found in Database: " + e.getMessage());
		}
		return result;
	}

	public <T> T find(Class<T> entityClass, Object ... equalFieldNamesAndValues) {
		if (equalFieldNamesAndValues.length % 2 != 0)
			throw new RuntimeException("Illegal arguments count: ${equalFieldNamesAndValues.length}");
		Map fieldValues = [:];
		for (int i = 0; i < equalFieldNamesAndValues.length;) {
			String field = (String) equalFieldNamesAndValues[i++];
			Object value = equalFieldNamesAndValues[i++];
			fieldValues["${field}"] = value;
		}
		return find(entityClass, fieldValues, null);
	}


	public Object find(String jpql, Object ... paramValues) {
		Query query = em.createQuery(jpql);
		for (int i = 0; i < paramValues.length; i++)
			query.setParameter(i + 1, paramValues[i]);
		return getSingleResult(query);
	}

	public boolean contains(Collection entities) {
		boolean res = true;
		entities.each {res &= contains(it)}
		return res
	}

	public boolean contains(Object entity) {
		return em.contains(entity);
	}

	private Object getSingleResult(Query query) {
		try {
			return query.getSingleResult()
		}
		catch (NoResultException e) {
			return null
		}
	}
}

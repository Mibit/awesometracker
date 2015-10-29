/*
 * Awesome Time Tracker project.
 * Licensed under Apache 2.0 License: http://www.apache.org/licenses/LICENSE-2.0
 * Author: Dmitry Zolotukhin <zlogic@gmail.com>
 */
package org.zlogic.att.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.zlogic.att.data.converters.Importer;

/**
 * Helper class to perform routine entity modifications and create a single
 * point where EntityManager is used.
 *
 * @author Dmitry Zolotukhin <a
 * href="mailto:zlogic@gmail.com">zlogic@gmail.com</a>
 */
public class PersistenceHelper {

	/**
	 * The logger
	 */
	private final static Logger log = Logger.getLogger(PersistenceHelper.class.getName());
	/**
	 * Localization messages
	 */
	private static final ResourceBundle messages = ResourceBundle.getBundle("org/zlogic/att/data/messages");
	/**
	 * Entity manager factory
	 */
	private EntityManagerFactory entityManagerFactory = null;
	/**
	 * True if shutdown is started. Disables any transactions.
	 */
	private boolean shuttingDown = false;
	/**
	 * Lock for shuttingDown
	 */
	private ReentrantReadWriteLock shuttingDownLock = new ReentrantReadWriteLock();

	/**
	 * Default constructor
	 */
	public PersistenceHelper() {
		entityManagerFactory = Persistence.createEntityManagerFactory("AwesomeTimeTrackerPersistenceUnit"); //NOI18N
	}

	/**
	 * Starts the shutdown and blocks any future requests to the database.
	 */
	public void shutdown() {
		try {
			shuttingDownLock.writeLock().lock();
			shuttingDown = true;
			entityManagerFactory.close();
		} finally {
			shuttingDownLock.writeLock().unlock();
		}
	}

	/**
	 * Closes an EntityManager, rolling back its transaction if it's still
	 * active
	 *
	 * @param entityManager EntityManager to close
	 */
	private void closeEntityManager(EntityManager entityManager) {
		if (entityManager == null || !entityManager.isOpen())
			return;
		if (entityManager.getTransaction().isActive()) {
			log.finer(messages.getString("ENTITYMANAGER_IS_STILL_ACTIVE_ROLLING_BACK_TRANSACTION"));
			entityManager.getTransaction().rollback();
		}
		entityManager.close();
	}

	/**
	 * Returns true if the application is shutting down and database requests
	 * will be ignored.
	 *
	 * @return true if the applications is shutting down
	 */
	public boolean isShuttingDown() {
		return shuttingDown;
	}

	/**
	 * Creates a Task entity
	 *
	 * @return the new Task entity, persisted in JPA
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public Task createTask() throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			Task task = createTask(entityManager);
			entityManager.getTransaction().commit();
			return task;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Creates a Task entity inside an existing EntityManager/transaction
	 *
	 * @param entityManager the EntityManager where the new Task will be
	 * persisted
	 * @return the new Task entity, persisted in JPA
	 */
	public Task createTask(EntityManager entityManager) {
		Task task = new Task();
		entityManager.persist(task);
		return task;
	}

	/**
	 * Creates a TimeSegment entity
	 *
	 * @param parent the parent Task
	 * @return the new TimeSegment entity, persisted in JPA
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public TimeSegment createTimeSegment(Task parent) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			TimeSegment segment = createTimeSegment(entityManager, parent);
			entityManager.getTransaction().commit();
			return segment;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Creates a TimeSegment entity inside an existing EntityManager/transaction
	 *
	 * @param entityManager the EntityManager where the new TimeSegment will be
	 * persisted
	 * @param parent the parent Task
	 * @return the new TimeSegment entity, persisted in JPA
	 */
	public TimeSegment createTimeSegment(EntityManager entityManager, Task parent) {
		parent = entityManager.find(Task.class, parent.getId());
		TimeSegment segment = parent.createSegment();
		entityManager.persist(segment);
		return segment;
	}

	/**
	 * Creates a CustomField entity
	 *
	 * @return the new CustomField entity, persisted in JPA
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public CustomField createCustomField() throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			CustomField customField = createCustomField(entityManager);
			entityManager.getTransaction().commit();
			return customField;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Creates a CustomField entity inside an existing EntityManager/transaction
	 *
	 * @param entityManager the EntityManager where the new CustomField will be
	 * persisted
	 * @return the new CustomField entity, persisted in JPA
	 */
	public CustomField createCustomField(EntityManager entityManager) {
		CustomField customField = new CustomField();
		entityManager.persist(customField);
		return customField;
	}

	/**
	 * Creates a FilterDate entity
	 *
	 * @param type the FilterDate type
	 * @return the new FilterDate entity, persisted in JPA
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public FilterDate createFilterDate(FilterDate.DateType type) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			FilterDate filter = createFilterDate(entityManager, type);
			entityManager.getTransaction().commit();
			return filter;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Creates a FilterDate entity inside an existing EntityManager/transaction
	 *
	 * @param entityManager the EntityManager where the new CustomField will be
	 * persisted
	 * @param type the FilterDate type
	 * @return the new FilterDate entity, persisted in JPA
	 */
	public FilterDate createFilterDate(EntityManager entityManager, FilterDate.DateType type) {
		FilterDate filter = new FilterDate(type);
		entityManager.persist(filter);
		return filter;
	}

	/**
	 * Creates a FilterCustomField entity
	 *
	 * @param customField the associated CustomField
	 * @return the new FilterCustomField entity, persisted in JPA
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public FilterCustomField createFilterCustomField(CustomField customField) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			FilterCustomField filter = createFilterCustomField(entityManager, customField);
			entityManager.getTransaction().commit();
			return filter;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Creates a FilterCustomField entity inside an existing
	 * EntityManager/transaction
	 *
	 * @param entityManager the EntityManager where the new CustomField will be
	 * persisted
	 * @param customField the associated CustomField
	 * @return the new FilterCustomField entity, persisted in JPA
	 */
	public FilterCustomField createFilterCustomField(EntityManager entityManager, CustomField customField) {
		FilterCustomField filter = new FilterCustomField(customField);
		entityManager.persist(filter);
		return filter;
	}

	/**
	 * Creates a createFilterTaskCompleted entity
	 *
	 * @return the new createFilterTaskCompleted entity, persisted in JPA
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public FilterTaskCompleted createFilterTaskCompleted() throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			FilterTaskCompleted filter = createFilterTaskCompleted(entityManager);
			entityManager.getTransaction().commit();
			return filter;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Creates a createFilterTaskCompleted entity inside an existing
	 * EntityManager/transaction
	 *
	 * @param entityManager the EntityManager where the new CustomField will be
	 * persisted
	 * @return the new createFilterTaskCompleted entity, persisted in JPA
	 */
	public FilterTaskCompleted createFilterTaskCompleted(EntityManager entityManager) {
		FilterTaskCompleted filter = new FilterTaskCompleted();
		entityManager.persist(filter);
		return filter;
	}

	/**
	 * Performs a requested change with a supplied TransactedChange. If process
	 * is shutting down, the change is ignored.
	 *
	 * @param requestedChange a TransactedChange implementation
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public void performTransactedChange(TransactedChange requestedChange) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			requestedChange.performChange(entityManager);
			entityManager.getTransaction().commit();
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Performs an EntityManager.merge operation in a new EntityManager
	 * instance/transaction
	 *
	 * @param entity the entity to be merged
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public void mergeEntity(Object entity) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.merge(entity);
			entityManager.getTransaction().commit();
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Returns all tasks from database
	 *
	 * @param applyFilters apply filters currently in the database to the
	 * resulting list
	 * @return all tasks from database
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public List<Task> getAllTasks(boolean applyFilters) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();

			List<Task> result = getAllTasks(entityManager, applyFilters);

			return result;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Returns all filters from database
	 *
	 * @return all filters from database
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public List<Filter> getAllFilters() throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();

			List<Filter> result = getAllFilters(entityManager);

			return result;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Returns all filters from database inside an existing
	 * EntityManager/transaction
	 *
	 * @param entityManager the EntityManager which will be used for lookup
	 * @return all filters from database
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public List<Filter> getAllFilters(EntityManager entityManager) throws ApplicationShuttingDownException {
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Filter> fitlersCriteriaQuery = criteriaBuilder.createQuery(Filter.class);
			fitlersCriteriaQuery.from(Filter.class);

			List<Filter> result = entityManager.createQuery(fitlersCriteriaQuery).getResultList();

			return result;
		} finally {
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Fetches the latest version of the task in the database, along with
	 * associated objects
	 *
	 * @param id the task's ID
	 * @return the task or null if it's not found
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public Task getTaskFromDatabase(long id) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();

			Task task = getTaskFromDatabase(id, entityManager);

			return task;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Fetches the latest version of the task in the database, along with
	 * associated objects inside an existing EntityManager/transaction
	 *
	 * @param id the task's ID
	 * @param entityManager the EntityManager which will be used for lookup
	 * @return the task or null if it's not found
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public Task getTaskFromDatabase(long id, EntityManager entityManager) throws ApplicationShuttingDownException {
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Task> tasksCriteriaQuery = criteriaBuilder.createQuery(Task.class);
			Root<Task> taskRoot = tasksCriteriaQuery.from(Task.class);

			tasksCriteriaQuery.where(criteriaBuilder.equal(taskRoot.get(Task_.id), id)).distinct(true);

			List<Task> result = entityManager.createQuery(tasksCriteriaQuery).getResultList();

			if (result.size() == 1)
				return result.get(0);
			else
				return null;
		} finally {
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Returns all tasks from database inside an existing
	 * EntityManager/transaction
	 *
	 * @param entityManager the EntityManager which will be used for lookup
	 * @param applyFilters apply filters currently in the database to the
	 * resulting list
	 * @return all tasks from database
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public List<Task> getAllTasks(EntityManager entityManager, boolean applyFilters) throws ApplicationShuttingDownException {
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Task> tasksCriteriaQuery = criteriaBuilder.createQuery(Task.class);
			Root<Task> taskRoot = tasksCriteriaQuery.from(Task.class);

			List<Filter> filters = getAllFilters();

			if (applyFilters) {
				Predicate filtersPredicate = criteriaBuilder.conjunction();
				for (Filter filter : filters)
					filtersPredicate = criteriaBuilder.and(filtersPredicate, filter.getFilterPredicate(criteriaBuilder, taskRoot));
				tasksCriteriaQuery.where(filtersPredicate);
			}
			tasksCriteriaQuery.distinct(true);

			List<Task> result = entityManager.createQuery(tasksCriteriaQuery).getResultList();

			//Apply empty filters
			if (applyFilters) {
				for (Filter filter : filters) {
					if (!(filter instanceof FilterCustomField))
						continue;
					FilterCustomField filterCustomField = (FilterCustomField) filter;
					if (!(filterCustomField.getCustomFieldValue() == null || filterCustomField.getCustomFieldValue().isEmpty()))
						continue;
					List<Task> tempResult = new LinkedList<>();
					for (Task task : result) {
						String customFieldValue = task.getCustomField(filterCustomField.getCustomField());
						if (customFieldValue == null || customFieldValue.isEmpty())
							tempResult.add(task);
					}
					result = tempResult;
				}
			}

			return result;
		} finally {
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Returns all custom fields from database
	 *
	 * @return all custom fields from database
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public List<CustomField> getCustomFields() throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();

			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<CustomField> fieldsCriteriaQuery = criteriaBuilder.createQuery(CustomField.class);
			fieldsCriteriaQuery.from(CustomField.class);

			List<CustomField> result = entityManager.createQuery(fieldsCriteriaQuery).getResultList();

			return result;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Returns all custom field values from database
	 *
	 * @return all custom field values from database
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public Map<CustomField, Set<String>> getAllCustomFieldValues() throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();

			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> fieldsCriteriaQuery = criteriaBuilder.createTupleQuery();
			Root<Task> taskRoot = fieldsCriteriaQuery.from(Task.class);
			Root<CustomField> customFieldRoot = fieldsCriteriaQuery.from(CustomField.class);
			MapJoin<Task, CustomField, String> customFieldJoin = taskRoot.join(Task_.customFields);
			customFieldJoin = customFieldJoin.on(criteriaBuilder.equal(customFieldJoin.key(), customFieldRoot));
			fieldsCriteriaQuery.multiselect(customFieldRoot, customFieldJoin.value()).distinct(true);

			List<Tuple> resultList = entityManager.createQuery(fieldsCriteriaQuery).getResultList();

			TreeMap<CustomField, Set<String>> result = new TreeMap<>();
			for (Tuple entry : resultList) {
				CustomField customField = entry.get(0, CustomField.class);
				String customFieldValue = entry.get(1, String.class);
				if (!result.containsKey(customField))
					result.put(customField, new TreeSet<>());
				result.get(customField).add(customFieldValue);
			}
			return result;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Returns a ConfigurationElement for its name
	 *
	 * @param name the ConfigurationElement name, or null if it doesn't exist
	 * @return the ConfigurationElement name
	 */
	public ConfigurationElement getConfigurationElement(String name) {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			ConfigurationElement result = entityManager.find(ConfigurationElement.class, name);
			return result;
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Prepares the EntityManager, transaction and calls the Importer's
	 * importData method with the created EntityManager
	 *
	 * @param importer the importer to be used
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public void importData(Importer importer) throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();

			importer.importData(this, entityManager);

			entityManager.getTransaction().commit();
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}

	/**
	 * Removes any orphaned entities
	 *
	 * @throws ApplicationShuttingDownException if application is shutting down
	 * and database requests are ignored
	 */
	public void cleanupDB() throws ApplicationShuttingDownException {
		EntityManager entityManager = null;
		try {
			shuttingDownLock.readLock().lock();
			if (shuttingDown)
				throw new ApplicationShuttingDownException();
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			List<Task> tasks = getAllTasks(entityManager, false);

			//Cleanup time segments
			Set<TimeSegment> ownedTimeSegments = new TreeSet<>();
			Set<TimeSegment> allTimeSegments = new TreeSet<>();
			for (Task task : tasks)
				ownedTimeSegments.addAll(task.getTimeSegments());

			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<TimeSegment> timeSegmentsCriteriaQuery = criteriaBuilder.createQuery(TimeSegment.class);
			timeSegmentsCriteriaQuery.from(TimeSegment.class);

			allTimeSegments.addAll(entityManager.createQuery(timeSegmentsCriteriaQuery).getResultList());
			allTimeSegments.removeAll(ownedTimeSegments);

			for (TimeSegment timeSegment : allTimeSegments)
				entityManager.remove(timeSegment);

			entityManager.getTransaction().commit();
		} finally {
			closeEntityManager(entityManager);
			shuttingDownLock.readLock().unlock();
		}
	}
}

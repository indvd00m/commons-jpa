package ru.indvdum.jpa.tests
;

import static org.junit.Assert.*

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Transient

import org.junit.Test

import ru.indvdum.jpa.dao.JPADataAccessObject
import ru.indvdum.jpa.entities.AbstractEntity;


/**
 * JUnit test case for testing of a creating, listing, updating and removing 
 * operations with database of the JPA entities.
 * 
 * @author 	indvdum (gotoindvdum@gmail.com)
 * @since 23.12.2011 22:54:26
 *
 */
abstract class AbstractJPAEntityTest extends AbstractJPATest {

	protected def uniqueValue = 1
	protected JPADataAccessObject dao = null
	protected Set toRemove = new HashSet()

	@Test
	public void testEntity() {
		dao = createDAO()
		try {
			testEntity(getEntityClass())
		} finally {
			dao.close()		
		}
	}
	
	/**
	 * @return Your implementation of JPADataAccessObject
	 */
	protected abstract JPADataAccessObject createDAO();

	/**
	 * Test creating, listing, updating, and removing of an {@code entityClass} object
	 * 
	 * @param entityClass
	 */
	protected void testEntity(Class entityClass) {
		
		assert AbstractEntity.class.isAssignableFrom(entityClass)

		Map<String, Object> fieldsValues
		def dbEntity

		// creating
		def entity = createEntity(entityClass)
		testCreatedEntity(entity)

		// listing
		toRemove.each {
			assert dao.list(it.class).each { it2 ->
				assert it.class.isAssignableFrom(it2.class)
			}.size() == toRemove.findAll { it2 ->
				it.class.isAssignableFrom(it2.class)
			}.size()
		}

		// updating
		fieldsValues = updateFields(entity)
		assert dao.persist(entity)
		assert dao.contains(entity)
		dbEntity = dao.find(entityClass, (entity as AbstractEntity).getIdentifierValue())
		assert dbEntity != null
		assert checkEntityFieldValues(dbEntity, fieldsValues)
		testUpdatedEntity(entity)

		// removing
		assert dao.remove(toRemove)
		assert !dao.contains(toRemove)
		testRemovedEntity(entity)
	}
	
	/**
	 * Create and persist to database an {@code entityClass} object
	 * 
	 * @param entityClass
	 * @return created entity object
	 */
	protected Object createEntity(Class entityClass) {
		assertNotNull entityClass.annotations.find {it instanceof Entity}
		def entity = entityClass.newInstance()
		assert entity.class == entityClass
		toRemove.add(entity)
		
		Map<String, Object> fieldsValues = updateFields(entity)
		assert dao.persist(entity)
		assert dao.contains(entity)
		def dbEntity = dao.find(entityClass, (entity as AbstractEntity).getIdentifierValue())
		assert dbEntity != null
		assert checkEntityFieldValues(dbEntity, fieldsValues)
		
		return entity
	}

	/**
	 * Update all fields of an {@code entity} object
	 * @param entity
	 * @return generated field values
	 */
	protected Map<String, Object> updateFields(Object entity) {
		Map<String, Object> fieldsValues = new HashMap<String, Object>()
		getFields(entity).grep {
			it.annotations.find {
				it instanceof Transient || it instanceof GeneratedValue
			} == null
		}.each {
			def newValue = generateFieldValue(entity, it)
			setFieldValueBySetter(entity, it, newValue)
			fieldsValues.put(it.name, newValue)
		}
		return fieldsValues
	}
	
	/**
	 * Collections and arrays will not be processed
	 * 
	 * @param entity
	 * @param field
	 * @return generated field value
	 */
	protected Object generateFieldValue(Object entity, Field field) {
		def type = field.getType()
		def newValue
		if(type.toString() == 'boolean' || type == Boolean.class) {
			newValue = (boolean) (uniqueValue++ % 2i == 0i)
		} else if(type.toString() == 'byte' || type == Byte) {
			newValue = (byte) (uniqueValue++ % Byte.MAX_VALUE + 1i)
		} else if(type.toString() == 'char' || type == Character) {
			newValue = (char) (uniqueValue++ % (int) Character.MAX_VALUE + 1i)
		} else if(type.toString() == 'short' || type == Short) {
			newValue = (short) (uniqueValue++ % Short.MAX_VALUE + 1i)
		} else if(type.toString() == 'int' || type == Integer) {
			newValue = (int) (uniqueValue++ % Integer.MAX_VALUE + 1i)
		} else if(type.toString() == 'long' || type == Long) {
			newValue = (long) uniqueValue++ % Long.MAX_VALUE + 1L
		} else if(type.toString() == 'float' || type == Float) {
			newValue = (float) uniqueValue++ % Float.MAX_VALUE + 1f
		} else if(type.toString() == 'double' || type == Double) {
			newValue = (double) uniqueValue++ % Double.MAX_VALUE + 1d
		} else if(type == String.class) {
			newValue = (String) "test${uniqueValue++}"
		} else if(type instanceof Class && (type as Class).annotations.find {it instanceof Entity} != null) { // modifying of a primary keys is deprecated
			// an attempt to use already created entities
			def currentValue = getFieldValue(entity, field)
			newValue = toRemove.find {it.class == type && it != currentValue}
			if(newValue == null)
				newValue = createEntity(type as Class)
		} else if(
				type instanceof Class 
				&& !(
					field.clazz.annotations.find {it instanceof IdClass} != null 
					&& field.declaredAnnotations.find {it instanceof Id} != null
				)
				&& field.declaredAnnotations.find {it instanceof EmbeddedId} == null
				&& !Collection.class.isAssignableFrom(type)
				&& !(type as Class).isArray()
			) { // modifying of a primary keys is deprecated
			newValue = (type as Class).newInstance()
		} else {
			newValue = getFieldValue(entity, field)
		}
		return newValue
	}

	/**
	 * @param entity
	 * @param rightValues
	 * @return {@code true}, if all entity fields is equals to {@code rightValues}
	 */
	protected boolean checkEntityFieldValues(Object entity, Map<String, Object> rightValues) {
		def result = true
		def fields = getFields(entity)
		rightValues.each {  key, value ->
			fields.find { it.name == key }.each {
				if(!getFieldValue(entity, it).equals(value)) {
					result = false
				}
			}
		}
		return result
	}

	/**
	 * Set a {@code field} to a {@code value} of an entity {@code object}
	 * 
	 * @param object
	 * @param field
	 * @param value
	 */
	protected void setFieldValue(Object object, Field field, Object value) {
		boolean isAccessible = field.accessible
		field.accessible = true
		field.set(object, value)
		field.accessible = isAccessible
	}
	
	/**
	 * Trying set a {@code field} to a {@code value} of an entity {@code object}
	 * by using a setter method. If setter not found, field will be set directly
	 * throw {@code setFieldValue} method.
	 * 
	 * @param object
	 * @param field
	 * @param value
	 */
	protected void setFieldValueBySetter(Object object, Field field, Object value) {
		boolean isSetted = false
		getMethods(object).grep {
			(
				Modifier.isPublic(it.modifiers) 
				&& it.parameterTypes.size() == 1 
				&& (
					value == null 
					|| it.parameterTypes[0].isAssignableFrom(value.class)
					)
				&& it.name =~ /^set(?i:${field.name.charAt(0)})${field.name.replaceFirst("^.{1}", "")}/
			)
		}.each {
			object."${it.name}"(value)
			isSetted = true
		}
		if(isSetted)
			return
		setFieldValue(object, field, value)
	}

	/**
	 * @param object
	 * @param field
	 * @return the field value of an object
	 */
	protected Object getFieldValue(Object object, Field field) {
		boolean isAccessible = field.accessible
		field.accessible = true
		def value = field.get(object)
		field.accessible = isAccessible
		return value
	}

	/**
	 * @param obj
	 * @return all object fields, including inherited
	 */
	protected Collection<Field> getFields(obj) {
		Class clazz = obj.getClass()
		Collection<Field> fields = clazz.declaredFields
		while(clazz.superclass != null) {
			clazz = clazz.superclass
			fields.addAll(clazz.declaredFields)
		}
		fields.grep {
			!it.synthetic &&
					!Modifier.isStatic(it.modifiers) &&
					!Modifier.isTransient(it.modifiers)
		}
	}

	/**
	 * @param obj
	 * @return all object methods, including inherited
	 */
	protected Collection<Method> getMethods(obj) {
		Class clazz = obj.getClass()
		Collection<Method> methods = clazz.declaredMethods
		while(clazz.superclass != null) {
			clazz = clazz.superclass
			methods.addAll(clazz.declaredMethods)
		}
		methods.grep {
			!it.synthetic &&
					!Modifier.isStatic(it.modifiers) &&
					!Modifier.isTransient(it.modifiers)
		}
	}
	
	/**
	 * For implement in successors
	 *
	 * @param entity
	 */
	protected void testCreatedEntity(Object entity) {
		
	}
	
	/**
	 * For implement in successors
	 *
	 * @param entity
	 */
	protected void testUpdatedEntity(Object entity) {
		
	}
	
	/**
	 * For implement in successors
	 *
	 * @param entity
	 */
	protected void testRemovedEntity(Object entity) {
		
	}

	/**
	 * @return an {@link AbstractEntity} successor
	 */
	abstract protected Class getEntityClass()
}

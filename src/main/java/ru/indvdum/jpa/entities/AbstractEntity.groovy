package ru.indvdum.jpa.entities

import java.lang.reflect.Modifier

import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Transient

import org.apache.openjpa.util.OpenJPAId
import org.codehaus.jackson.map.ObjectMapper

import ru.indvdum.jpa.dao.JPADataAccessObject;

/**
 * @author 	indvdum (gotoindvdum@gmail.com)
 * @since 25.12.2011 13:26:57
 *
 */
abstract class AbstractEntity implements Serializable {

	@Transient
	protected ObjectMapper mapper = new ObjectMapper()

	@Override
	public String toString() {
		return getClass().getSimpleName() + '-' + getIdentifierValue()
	}

	/**
	 * JSON-like representation of an object
	 * 
	 * @return
	 */
	public String toJSON() {
		return mapper.writeValueAsString(this)
	}

	/* 
	 * HashCode, based on root class name of entity class hierarchy and id value 
	 * 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		Class clazz = getClass()
		while (
			clazz.superclass != null
			&& !Modifier.isAbstract(clazz.superclass.modifiers)
			&& clazz.superclass.annotations.find {it instanceof Entity} != null
		)
			clazz = clazz.superclass
		String res = clazz.getName() + ': ' + getIdentifierValue()
		return res.hashCode()
	}

	/* 
	 * Comparison, based on entities id's
	 * 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false
		if(!(obj.class.isAssignableFrom(this.class) || this.class.isAssignableFrom(obj.class)))
			return false
		def id = getIdentifierValue()
		def objId = (obj as AbstractEntity).getIdentifierValue()
		return id == objId || id != null && id != this && id.equals(objId)
	}
	
	/**
	 * @return The identifier value of an entity.
	 */
	@Transient
	public Object getIdentifierValue() {
		// an attempt to get id by EntityManagerFactory
		Object identifier = JPADataAccessObject.emf.getPersistenceUnitUtil().getIdentifier(this)
		if(identifier instanceof OpenJPAId)
			return ((OpenJPAId)identifier).getIdObject()
		
		def result
		
		// an attempt to find id by annotated fields
		Class clazz = getClass()
		def self = this
		Collection fields = clazz.declaredFields
		while(clazz.superclass != null) {
			clazz = clazz.superclass
			fields.addAll(clazz.declaredFields)
		}
		fields.grep {
			!it.synthetic &&
					!Modifier.isStatic(it.modifiers) &&
					!Modifier.isTransient(it.modifiers)
		}.grep {
			it.annotations.find {
				it instanceof Id || it instanceof EmbeddedId
			} != null
		}.each {
			boolean isAccessible = it.accessible
			it.accessible = true
			result = it.get(self)
			it.accessible = isAccessible
		}
		if(result != null && !(result instanceof Reference && (result as Reference).value == null))
			return result
		
		// an attempt to represent id as a aggregate of all fields
		// TODO check, why toJSON method don't work
//		return toJSON()
		result = '{'
		fields.grep {
			!it.synthetic &&
					!Modifier.isStatic(it.modifiers) &&
					!Modifier.isTransient(it.modifiers)
		}.grep {
			it.annotations.find {
				it instanceof Transient
			} == null
		}.each {
			boolean isAccessible = it.accessible
			it.accessible = true
			result += '"' + it.name + '": "' + it.get(self) + '", '
			it.accessible = isAccessible
		}
		return result
	}
	
	protected def callMethod(method, Object ... args) {
		"$method"(*args)
	}
}

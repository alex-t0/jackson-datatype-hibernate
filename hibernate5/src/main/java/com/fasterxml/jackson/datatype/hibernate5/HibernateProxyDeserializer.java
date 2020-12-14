package com.fasterxml.jackson.datatype.hibernate5;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;

import org.hibernate.SessionFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class HibernateProxyDeserializer extends JsonDeserializer<Object> implements ResolvableDeserializer {

	private final JsonDeserializer<?> _defaultDeserializer;
	
	protected SessionFactory _sessionFactory = null;
	
	protected EntityManager _entityManager = null;
	
	private static Object _lockObject = new Object();
	
	public EntityManager getEntityManager() {
		
		if (this._entityManager != null && this._entityManager.isOpen())
			return this._entityManager;
		
		synchronized (_lockObject) {
			if (this._entityManager == null || !this._entityManager.isOpen())
				this._entityManager = _sessionFactory.createEntityManager();
			
			return this._entityManager;
		}
	}

	public HibernateProxyDeserializer(SessionFactory _sessionFactory, JsonDeserializer<?> defaultDeserializer) {
		this._defaultDeserializer = defaultDeserializer;
		// super(Object.class)
		this._sessionFactory = _sessionFactory;
	}
	
	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {

		return _defaultDeserializer.deserialize(p, ctxt);
		// throw new NotYetImplementedException(HibernateProxyDeserializer.class.getName() + " can only work with type deserialization");
	}

	@Override
	public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer,
			Object intoValue) throws IOException {
		
		Object rawValue = super.deserializeWithType(p, ctxt, typeDeserializer, intoValue);
		
		if (!isEntityObject(rawValue)) return rawValue; 
		
		Field idField = getIdField(rawValue);
		
		try {
			if (!seemsWithIdOnly(rawValue, idField)) return rawValue;
		} catch (Throwable e1) {
			return rawValue;
		}
		
		idField.setAccessible(true);
		
		Object id;
		try {
			id = idField.get(rawValue);
		} catch (Throwable e) {
			return rawValue;
		}
		
		EntityManager em = getEntityManager();
		
		return em.getReference(rawValue.getClass(), id);
	}

	@Override
	public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
			throws IOException {
		Object rawValue = super.deserializeWithType(p, ctxt, typeDeserializer);
		
		if (!isEntityObject(rawValue)) return rawValue; 
		
		Field idField = getIdField(rawValue);
		
		try {
			if (!seemsWithIdOnly(rawValue, idField)) return rawValue;
		} catch (Throwable e1) {
			return rawValue;
		}
		
		idField.setAccessible(true);
		
		Object id;
		try {
			id = idField.get(rawValue);
		} catch (Throwable e) {
			return rawValue;
		}
		
		EntityManager em = getEntityManager();
		
		return em.getReference(rawValue.getClass(), id);
	}
	
	private Field getIdField(Object entityObject) {
		Class<?> clazz = entityObject.getClass();
		
		for(Field field : clazz.getDeclaredFields()){
		  // Class type = field.getType();
		  // String name = field.getName();
		  Annotation[] annotations = field.getDeclaredAnnotations();
		  for (Annotation annotation: annotations) {
			  if (Id.class.isAssignableFrom(annotation.getClass())) 
				  return field;
		  }
		}
		
		return null;
	}
	
	private boolean isEntityObject(Object entityObject) {
		Class<?> clazz = entityObject.getClass();
		
		Entity entityAnnotation = clazz.getAnnotation(Entity.class);
		
		 return entityAnnotation != null;
	}
	
	private boolean seemsWithIdOnly(Object entityObject, Field idField) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = entityObject.getClass();
		
		for(Field field : clazz.getDeclaredFields()) {
			if (field.equals(idField)) continue;
			
			field.setAccessible(true);
			
			if (field.get(entityObject) != null) return false;
		}
		
		idField.setAccessible(true);
		return idField.get(entityObject) != null;
	}

	@Override
	public void resolve(DeserializationContext ctxt) throws JsonMappingException {
		((ResolvableDeserializer) _defaultDeserializer).resolve(ctxt);	
	}
}

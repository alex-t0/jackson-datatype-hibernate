package com.fasterxml.jackson.datatype.hibernate5;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

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
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

public class HibernateProxyDeserializer extends JsonDeserializer<Object> implements ResolvableDeserializer {

	private final JsonDeserializer<?> _defaultDeserializer;
	
	protected SessionFactoryProvider _sessionFactoryProvider = null;
	
	protected EntityManager _entityManager = null;
	
	private static Object _lockObject = new Object();
	
	// private static Logger log = Logger.getLogger(HibernateProxyDeserializer.class);
	
	public EntityManager getEntityManager() {
		
		if (this._entityManager != null && this._entityManager.isOpen())
			return this._entityManager;
		
		synchronized (_lockObject) {
			EntityManager fromStorage = SessionFactoryStorageHolder.getEntityManager();
			if (fromStorage != null && fromStorage.isOpen()) {
				this._entityManager = fromStorage;
				return this._entityManager;
			}
			
			if (this._entityManager == null || !this._entityManager.isOpen()) {
				if (_sessionFactoryProvider == null) {
					// log.info("_sessionFactoryProvider is null");
				}
				
				SessionFactory sessionFactory = _sessionFactoryProvider.getSessionFactory();
				
				if (sessionFactory == null) {
					// log.info("sessionFactory is null");
				}
				
				// EntityManager em = sessionFactory.getCurrentSession();
				
				// if (em == null) em = sessionFactory.createEntityManager();
					
				EntityManager em = sessionFactory.createEntityManager();
				
				SessionFactoryStorageHolder.setEntityManager(em);
				
				this._entityManager = em;
			}
			
			return this._entityManager;
		}
		
		/*if (_sessionFactoryProvider == null) return null;
		
		SessionFactory sessionFactory = _sessionFactoryProvider.getSessionFactory();
		
		if (sessionFactory == null) return null;
		
		return sessionFactory.createEntityManager();*/
	}

	public HibernateProxyDeserializer(SessionFactoryProvider _sessionFactoryProvider, JsonDeserializer<?> defaultDeserializer) {
		this._defaultDeserializer = defaultDeserializer;
		// super(Object.class)
		this._sessionFactoryProvider = _sessionFactoryProvider;
	}
	
	@Override
	public SettableBeanProperty findBackReference(String refName) {
		return this._defaultDeserializer.findBackReference(refName);
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
		
		// Object rawValue = super.deserializeWithType(p, ctxt, typeDeserializer, intoValue);
		@SuppressWarnings("unchecked")
		Object rawValue = ((JsonDeserializer<Object>)this._defaultDeserializer).deserializeWithType(p, ctxt, typeDeserializer, (Object) intoValue);
		
		if (_sessionFactoryProvider == null) return rawValue;
		
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
		
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		ClassLoader current = rawValue.getClass().getClassLoader();
		Thread.currentThread().setContextClassLoader(current);
		EntityManager em = getEntityManager();
		Thread.currentThread().setContextClassLoader(tccl);
		
		return em.getReference(rawValue.getClass(), id); 
	}

	@Override
	public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
			throws IOException {
		// Object rawValue = super.deserializeWithType(p, ctxt, typeDeserializer);
		Object rawValue = this._defaultDeserializer.deserializeWithType(p, ctxt, typeDeserializer);
		
		if (_sessionFactoryProvider == null) return rawValue;
		
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
		
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		ClassLoader current = rawValue.getClass().getClassLoader();
		Thread.currentThread().setContextClassLoader(current);
		EntityManager em = getEntityManager();
		Thread.currentThread().setContextClassLoader(tccl);
		
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
			if ("serialVersionUID".equals(field.getName())) continue;
			if ("lastModifiedBy".equals(field.getName())) continue;
			
			field.setAccessible(true);
			
			if (field.getType().equals(Boolean.class)) continue;
			
			if (field.getType().equals(String.class) && "".equals(field.get(entityObject))) continue;
			
			if (field.getType().isArray()) {
				Object[] arr = ((Object[])field.get(entityObject));
				
				if (arr == null || arr.length == 0) continue;
			}
			
			Class<?> collectionClass = Collection.class;
			
			if (collectionClass.isAssignableFrom(field.getType())) {
				Collection<?> c = ((Collection<?>)field.get(entityObject));
				
				if (c == null || c.size() == 0) continue;
			}
			
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

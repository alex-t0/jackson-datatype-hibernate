package com.fasterxml.jackson.datatype.hibernate5;

public class HibernateProxyWrapper {

	private Class<?> wrappedClass;
	private Object Id;
	
	public HibernateProxyWrapper(Class<?> wrappedClass, Object id) {
		super();
		this.wrappedClass = wrappedClass;
		Id = id;
	}
	
	public Class<?> getWrappedClass() {
		return wrappedClass;
	}
	public void setWrappedClass(Class<?> wrappedClass) {
		this.wrappedClass = wrappedClass;
	}
	
	public Object getId() {
		return Id;
	}
	public void setId(Object id) {
		Id = id;
	}
}

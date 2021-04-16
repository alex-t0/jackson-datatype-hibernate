package com.fasterxml.jackson.datatype.hibernate5;

import org.hibernate.SessionFactory;

@FunctionalInterface
public interface SessionFactoryProvider {
	SessionFactory getSessionFactory();
}

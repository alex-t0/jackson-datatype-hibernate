package com.fasterxml.jackson.datatype.hibernate5;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

public class SessionFactoryStorageHolder {
	private static final ThreadLocal<EntityManager> threadLocalEntityManager = new  ThreadLocal<>();
	
	private static ClassLoader containerClassloader = null;
	
	private static Logger log = Logger.getLogger(SessionFactoryStorageHolder.class);
	
	public final static EntityManager getEntityManager() {
		return threadLocalEntityManager.get();
	}
	
	// public static ArrayList<EntityManagerItem> ems = new ArrayList<>();
	// public static Object lock = new Object();
	
	public final static void setEntityManager(EntityManager em) {
		EntityManager current = threadLocalEntityManager.get();
		
		/* synchronized (lock) {
			ArrayList<EntityManagerItem> emsCleanup = new ArrayList<>();		
			for (EntityManagerItem emCycle : ems) {
				if (!emCycle.em.isOpen() || Math.abs(emCycle.date.getTime() - (new Date()).getTime()) > 120 * 1000) 
					emsCleanup.add(emCycle);
			}
			
			for (EntityManagerItem emCycle : emsCleanup) {
				if (emCycle.em.isOpen()) emCycle.em.close();
				
				ems.remove(emCycle);
				log.debug("Removed " + 
							emCycle.em + " (" + 
							(emCycle.em.isOpen() ? "open" : "closed") + 
							") inserted at " + 
							new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(emCycle.date));
				log.debug("Total " + ems.size() + " items");
			}
		} */
		
		if (em == null)
			threadLocalEntityManager.remove();
		else {
			if (current != null)
				log.debug("WARNING: replacing not null EntityManager!");
			
			/* synchronized (lock) {
				EntityManagerItem item = new EntityManagerItem();
				item.date = new Date();
				item.em = em;
				
				ems.add(item);
				log.debug("Inserted new item. Total " + ems.size() + " items");
			} */
			
			threadLocalEntityManager.set(em);
		}
	}

	public final static ClassLoader getClassLoader() {
		return containerClassloader;
	}
	
	public final static void setClassLoader(ClassLoader cl) {
		containerClassloader = cl;
	}
	
}

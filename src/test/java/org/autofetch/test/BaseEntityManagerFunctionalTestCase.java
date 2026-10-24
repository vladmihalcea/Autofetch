/**
 * This file is part of Autofetch, and is distributed under the terms of the Lesser GNU General Public License.
 * <p>
 * Hibernate 7 port (2026): replaces the Hibernate 5.3 hibernate-testing base class, which Hibernate 7 no longer
 * provides in that form, with a JPA 3.2 bootstrap on an in-memory H2 database. As before, every test method gets a
 * new EntityManagerFactory.
 */
package org.autofetch.test;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;

public abstract class BaseEntityManagerFunctionalTestCase {

	private SessionFactoryImplementor entityManagerFactory;

	protected SessionFactoryImplementor entityManagerFactory() {
		return entityManagerFactory;
	}

	protected ServiceRegistry serviceRegistry() {
		return entityManagerFactory.getServiceRegistry();
	}

	@Before
	public void buildEntityManagerFactory() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 5 );
		addConfigOptions( settings );

		HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration( getClass().getSimpleName() )
				.jdbcUrl( "jdbc:h2:mem:" + getClass().getSimpleName() + ";DB_CLOSE_DELAY=-1" )
				.jdbcCredentials( "sa", "" )
				.properties( settings );
		for ( Class<?> annotatedClass : getAnnotatedClasses() ) {
			configuration.managedClass( annotatedClass );
		}
		entityManagerFactory = configuration.createEntityManagerFactory().unwrap( SessionFactoryImplementor.class );

		afterEntityManagerFactoryBuilt();
	}

	@After
	public void releaseResources() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
	}

	protected Session getOrCreateEntityManager() {
		return entityManagerFactory.openSession();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[0];
	}

	protected void addConfigOptions(Map<String, Object> options) {
	}

	protected void afterEntityManagerFactoryBuilt() {
	}
}

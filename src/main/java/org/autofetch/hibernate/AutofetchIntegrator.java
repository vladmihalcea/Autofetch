/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): the Hibernate 7 Integrator contract; the service is contributed by AutofetchServiceContributor
 * only, and the session factory is kept by the service instead of a static field.
 */
package org.autofetch.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;

public class AutofetchIntegrator implements Integrator {

	@Override
	public void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		AutofetchService service = sessionFactory.getServiceRegistry().requireService( AutofetchService.class );
		service.setSessionFactory( sessionFactory );
		final ExtentManager extentManager = service.getExtentManager();
		EventListenerRegistry eventListenerRegistry = sessionFactory.getEventListenerRegistry();
		eventListenerRegistry.setListeners( EventType.LOAD, new AutofetchLoadListener( extentManager ) );
		eventListenerRegistry.setListeners(
				EventType.INIT_COLLECTION,
				new AutofetchInitializeCollectionListener( extentManager )
		);
	}
}

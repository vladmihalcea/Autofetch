/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): the service also holds the entity proxy factory and the session factory, which the Hibernate 5.3
 * version reached through its tuplizer and a static field of the integrator.
 */
package org.autofetch.hibernate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;

import java.io.Serializable;

public interface AutofetchService extends Service, Serializable {

    ExtentManager getExtentManager();

    EntityProxyFactory getEntityProxyFactory();

    SessionFactoryImplementor getSessionFactory();

    void setSessionFactory(SessionFactoryImplementor sessionFactory);
}

/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): new. Hibernate 7 has no pluggable entity instantiator, so Autofetch installs a session-scoped
 * AutofetchInterceptor, whose instantiate method Hibernate consults before the default constructor.
 */
package org.autofetch.hibernate;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;

public class AutofetchSessionFactoryBuilderFactory implements SessionFactoryBuilderFactory {

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder(
			MetadataImplementor metadata,
			SessionFactoryBuilderImplementor defaultBuilder) {
		AutofetchService service = metadata.getMetadataBuildingOptions()
				.getServiceRegistry()
				.requireService( AutofetchService.class );
		defaultBuilder.applyStatelessInterceptor( () -> new AutofetchInterceptor( service ) );
		return defaultBuilder;
	}
}

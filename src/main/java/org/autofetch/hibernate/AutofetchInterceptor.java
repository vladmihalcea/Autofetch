/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): new. It takes over the two jobs of the Hibernate 5.3 AutofetchTuplizer: it instantiates
 * entities as tracking subclasses (AutofetchInstantiator), and, because Hibernate 7 creates its lazy to-one
 * proxies itself (AutofetchProxyFactory), it keeps the trackers of an uninitialized proxy until its entity is
 * instantiated.
 */
package org.autofetch.hibernate;

import org.hibernate.Interceptor;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.proxy.LazyInitializer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AutofetchInterceptor implements Interceptor, Serializable {

	private static final long serialVersionUID = 1L;

	private final AutofetchService service;

	private final Map<EntityReference, Set<Statistics>> pendingTrackers = new HashMap<>();

	public AutofetchInterceptor(AutofetchService service) {
		this.service = service;
	}

	@Override
	public Object instantiate(String entityName, EntityRepresentationStrategy representationStrategy, Object id) {
		if ( representationStrategy.getMode() != RepresentationMode.POJO ) {
			return null;
		}
		Object instance = service.getEntityProxyFactory().newInstance( entityName );
		if ( instance == null ) {
			return null;
		}
		if ( id != null ) {
			service.getSessionFactory()
					.getMappingMetamodel()
					.getEntityDescriptor( entityName )
					.setIdentifier( instance, id, null );
		}
		Set<Statistics> trackers = pendingTrackers.remove( new EntityReference( entityName, id ) );
		if ( trackers != null ) {
			( (TrackableEntity) instance ).addTrackers( trackers );
		}
		return instance;
	}

	@Override
	public String getEntityName(Object object) {
		return object instanceof TrackableEntity
				? service.getEntityProxyFactory().getEntityName( object.getClass() )
				: null;
	}

	/**
	 * Attaches a tracker to the entity behind an uninitialized proxy: the tracker is added when the entity is
	 * instantiated, which happens when the proxy is initialized.
	 */
	static boolean addPendingTracker(LazyInitializer lazyInitializer, Statistics tracker) {
		if ( lazyInitializer.getSession() != null
				&& lazyInitializer.getSession().getInterceptor() instanceof AutofetchInterceptor ) {
			AutofetchInterceptor interceptor = (AutofetchInterceptor) lazyInitializer.getSession().getInterceptor();
			interceptor.pendingTrackers
					.computeIfAbsent(
							new EntityReference( lazyInitializer.getEntityName(), lazyInitializer.getInternalIdentifier() ),
							key -> new HashSet<>()
					)
					.add( tracker );
			return true;
		}
		return false;
	}

	private static final class EntityReference implements Serializable {

		private final String entityName;

		private final Object id;

		EntityReference(String entityName, Object id) {
			this.entityName = entityName;
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof EntityReference ) ) {
				return false;
			}
			EntityReference other = (EntityReference) o;
			return entityName.equals( other.entityName ) && Objects.equals( id, other.id );
		}

		@Override
		public int hashCode() {
			return 31 * entityName.hashCode() + Objects.hashCode( id );
		}
	}
}

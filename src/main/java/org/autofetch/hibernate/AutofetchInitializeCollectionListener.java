/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): extends the Hibernate 7 DefaultInitializeCollectionEventListener; without prefetch paths, or
 * when the collection is cached, it delegates to the default listener (which reads the second-level cache)
 * instead of copying its cache lookup.
 */
package org.autofetch.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.event.internal.DefaultInitializeCollectionEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.persister.collection.CollectionPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutofetchInitializeCollectionListener extends DefaultInitializeCollectionEventListener {

	private static final Logger log = LoggerFactory.getLogger( AutofetchInitializeCollectionListener.class );

	private final ExtentManager extentManager;

	public AutofetchInitializeCollectionListener(ExtentManager extentManager) {
		this.extentManager = extentManager;
	}

	@Override
	public void onInitializeCollection(InitializeCollectionEvent event) throws HibernateException {
		PersistentCollection<?> collection = event.getCollection();
		EventSource source = event.getSession();
		CollectionEntry ce = source.getPersistenceContextInternal().getCollectionEntry( collection );
		if ( ce == null ) {
			throw new HibernateException( "collection was evicted" );
		}
		if ( !collection.wasInitialized() ) {
			CollectionPersister cp = ce.getLoadedPersister();
			String classname = cp.getOwnerEntityPersister().getEntityName();
			String tpKey = cp.getRole();
			List<Path> prefetchPaths = extentManager.getPrefetchPaths( tpKey );
			boolean useCache = cp.hasCache() && source.getCacheMode().isGetEnabled();
			if ( !prefetchPaths.isEmpty() && !useCache ) {
				String assoc = tpKey.substring( tpKey.lastIndexOf( '.' ) + 1 );
				List<Path> augmentedPaths = new ArrayList<>();
				augmentedPaths.add( new Path().addTraversal( assoc ) );
				for ( Path p : prefetchPaths ) {
					augmentedPaths.add( p.prependTraversal( assoc ) );
				}
				log.debug( "Prefetch paths: " + augmentedPaths );
				AutofetchLoadListener.getResult(
						augmentedPaths,
						classname,
						ce.getLoadedKey(),
						LockMode.NONE,
						source
				);
				if ( !collection.wasInitialized() ) {
					throw new IllegalStateException( "Collection not initialized" );
				}
			}
			else {
				super.onInitializeCollection( event );
			}

			boolean oldTracking = false;
			if ( collection instanceof Trackable ) {
				Trackable trackable = (Trackable) collection;
				oldTracking = trackable.disableTracking();
			}
			Iterator<?> elementsIter = collection.entries( cp );
			while ( elementsIter.hasNext() ) {
				extentManager.markAsRoot( elementsIter.next(), tpKey );
			}
			if ( collection instanceof Trackable ) {
				Trackable trackable = (Trackable) collection;
				if ( oldTracking ) {
					trackable.enableTracking();
				}
			}
			log.trace( "collection initialized" );
		}
	}
}

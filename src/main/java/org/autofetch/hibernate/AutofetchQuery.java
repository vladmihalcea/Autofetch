/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): replaces AutofetchCriteria, because Hibernate 6 removed the legacy Criteria API. It wraps a JPA
 * TypedQuery (JPQL, HQL, or a JPA CriteriaQuery) whose selection is its single root entity, applies the prefetch
 * paths as a load graph (instead of FetchMode.JOIN), and marks the results as roots, as AutofetchCriteria did.
 * Configure the query before wrapping it; the wrapper only runs it.
 */
package org.autofetch.hibernate;

import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.graph.Graph;
import org.hibernate.graph.RootGraph;
import org.hibernate.jpa.SpecHints;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutofetchQuery<T> {

	private static final Logger log = LoggerFactory.getLogger( AutofetchQuery.class );

	private final Session session;

	private final TypedQuery<T> delegate;

	private final SqmRoot<?> root;

	public AutofetchQuery(Session session, TypedQuery<T> query) {
		this.session = session;
		this.delegate = query;
		this.root = selectedRoot( query );
	}

	public List<T> getResultList() {
		if ( root == null ) {
			return delegate.getResultList();
		}
		String classname = root.getEntityName();
		addPrefetch( classname );
		List<T> results = delegate.getResultList();
		for ( Object o : results ) {
			getExtentManager().markAsRoot( o, classname );
		}
		return results;
	}

	public T getSingleResult() {
		if ( root == null ) {
			return delegate.getSingleResult();
		}
		String classname = root.getEntityName();
		addPrefetch( classname );
		T o = delegate.getSingleResult();
		getExtentManager().markAsRoot( o, classname );
		return o;
	}

	public TypedQuery<T> getDelegate() {
		return delegate;
	}

	private void addPrefetch(String tpKey) {
		List<Path> prefetchPaths = getExtentManager().getPrefetchPaths( tpKey );
		if ( log.isDebugEnabled() ) {
			log.debug( "Prefetch paths for " + tpKey + ": " + prefetchPaths );
		}
		if ( prefetchPaths.isEmpty() ) {
			return;
		}
		RootGraph<?> graph = session.createEntityGraph( root.getJavaType() );
		Map<Path, Graph<?>> graphs = new HashMap<>();
		graphs.put( new Path(), graph );
		// Like AutofetchLoadListener.getResult, assumes shorter paths come before the paths they prefix
		for ( Path p : prefetchPaths ) {
			Graph<?> parent = graphs.get( p.removeLastTraversal() );
			if ( parent == null ) {
				continue;
			}
			String lastField = p.traversals().get( p.size() - 1 );
			graphs.put( p, parent.addSubgraph( lastField ) );
		}
		delegate.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH, graph );
	}

	private ExtentManager getExtentManager() {
		return session.getSessionFactory()
				.unwrap( org.hibernate.engine.spi.SessionFactoryImplementor.class )
				.getServiceRegistry()
				.requireService( AutofetchService.class )
				.getExtentManager();
	}

	/**
	 * Returns the query's root when the query selects exactly that root, the case of the root entity result
	 * transformers AutofetchCriteria handled; otherwise {@code null}, and the query runs unchanged.
	 */
	private static SqmRoot<?> selectedRoot(TypedQuery<?> query) {
		SqmStatement<?> statement;
		try {
			statement = query.unwrap( SqmQuery.class ).getSqmStatement();
		}
		catch (RuntimeException e) {
			return null;
		}
		if ( !( statement instanceof SqmSelectStatement ) ) {
			return null;
		}
		SqmQuerySpec<?> spec = ( (SqmSelectStatement<?>) statement ).getQuerySpec();
		if ( spec.getRootList().size() != 1 ) {
			return null;
		}
		SqmRoot<?> root = spec.getRootList().get( 0 );
		List<? extends SqmSelection<?>> selections = spec.getSelectClause().getSelections();
		if ( selections.isEmpty() ) {
			return root;
		}
		if ( selections.size() == 1 && selections.get( 0 ).getSelectableNode() == root ) {
			return root;
		}
		return null;
	}
}

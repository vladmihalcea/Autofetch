/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): the Hibernate 7 UserCollectionType contract.
 */
package org.autofetch.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class AutofetchSetType implements UserCollectionType {

	@Override
	public CollectionClassification getClassification() {
		return CollectionClassification.SET;
	}

	@Override
	public Class<?> getCollectionClass() {
		return java.util.Set.class;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
				? new java.util.HashSet<>()
				: new java.util.HashSet<>( anticipatedSize + (int) ( anticipatedSize * .75f ), .75f );
	}

	@Override
	public PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
			throws HibernateException {
		return new AutofetchSet<>( session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		return new AutofetchSet<>( session, (java.util.Set<Object>) collection );
	}

	@Override
	public Iterator<?> getElementsIterator(Object collection) {
		return ( (Collection<?>) collection ).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ( (Collection<?>) collection ).contains( entity );
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Object replaceElements(
			Object original, Object target, CollectionPersister persister, Object owner,
			Map copyCache, SharedSessionContractImplementor session) throws HibernateException {
		( (Collection) target ).clear();
		( (Collection) target ).addAll( (Collection<?>) original );
		return target;
	}
}

/**
 * Copyright 2008 Ali Ibrahim
 * <p>
 * This file is part of Autofetch. Autofetch is free software: you can redistribute it and/or modify it under the terms of the Lesser GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Autofetch is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the Lesser GNU General Public License for more details. You should have received a copy of the Lesser GNU General Public
 * License along with Autofetch.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Hibernate 7 port (2026): the Hibernate 7 PersistentCollection API; every read or write method still calls the
 * superclass first and then records the access.
 */
package org.autofetch.hibernate;

import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.Collection;
import java.util.Iterator;

import java.util.Set;

public class AutofetchSet<E> extends PersistentSet<E> implements Trackable {

    private final CollectionTracker collectionTracker = new CollectionTracker();

    @SuppressWarnings("unused")
    protected AutofetchSet() {
        // Available only for serialization.
    }

    public AutofetchSet(SharedSessionContractImplementor session) {
        super(session);
    }

    public AutofetchSet(SharedSessionContractImplementor session, Set<E> collection) {
        super(session, collection);
    }

    @Override
    public void addTracker(Statistics tracker) {
        this.collectionTracker.addTracker(tracker);
    }

    @Override
    public void addTrackers(Set<Statistics> trackers) {
        this.collectionTracker.addTrackers(trackers);
    }

    @Override
    public boolean disableTracking() {
        boolean oldValue = this.collectionTracker.isTracking();
        this.collectionTracker.setTracking(false);
        return oldValue;
    }

    @Override
    public boolean enableTracking() {
        boolean oldValue = this.collectionTracker.isTracking();
        this.collectionTracker.setTracking(true);
        return oldValue;
    }

    @Override
    public void removeTracker(Statistics stats) {
        this.collectionTracker.removeTracker(stats);
    }

    @Override
    public boolean isAccessed() {
        return this.collectionTracker.isAccessed();
    }

    @Override
    public int size() {
        int ret = super.size();
        this.accessed();
        return ret;
    }

    @Override
    public boolean isEmpty() {
        boolean ret = super.isEmpty();
        this.accessed();
        return ret;
    }

    @Override
    public boolean contains(Object object) {
        boolean ret = super.contains(object);
        this.accessed();
        return ret;
    }

    @Override
    public Iterator<E> iterator() {
        Iterator<E> iter = super.iterator();
        this.accessed();
        return iter;
    }

    @Override
    public Object[] toArray() {
        Object[] arr = super.toArray();
        this.accessed();
        return arr;
    }

    @Override
    public <A> A[] toArray(A[] array) {
        A[] arr = super.toArray(array);
        this.accessed();
        return arr;
    }

    @Override
    public boolean add(E value) {
        boolean ret = super.add(value);
        this.accessed();
        return ret;
    }

    @Override
    public boolean remove(Object value) {
        boolean ret = super.remove(value);
        this.accessed();
        return ret;
    }

    @Override
    public boolean containsAll(Collection<?> coll) {
        boolean ret = super.containsAll(coll);
        this.accessed();
        return ret;
    }

    @Override
    public boolean addAll(Collection<? extends E> coll) {
        boolean ret = super.addAll(coll);
        this.accessed();
        return ret;
    }

    @Override
    public void clear() {
        super.clear();
        this.accessed();
    }

    @Override
    public boolean retainAll(Collection<?> coll) {
        boolean ret = super.retainAll(coll);
        this.accessed();
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> coll) {
        boolean ret = super.removeAll(coll);
        this.accessed();
        return ret;
    }

    @Override
    public String toString() {
        String ret = super.toString();
        this.accessed();
        return ret;
    }

    @Override
    public boolean equals(Object other) {
        boolean ret = super.equals(other);
        this.accessed();
        return ret;
    }

    @Override
    public int hashCode() {
        int ret = super.hashCode();
        this.accessed();
        return ret;
    }

    private void accessed() {
        if (wasInitialized()) {
            this.collectionTracker.trackAccess(this.set);
        }
    }
}

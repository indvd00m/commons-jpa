package ru.indvdum.jpa.collections

import java.util.Collection
import java.util.Iterator
import java.util.List
import java.util.ListIterator

/**
 * E realization of List interface with support of change notifications 
 * 
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 12.01.2012 13:57:48
 *
 * @param <E>
 */
abstract class TriggeredList<E> extends ArrayList<E> {
	
	/**
	 * Invoked after changes in list
	 */
	abstract protected void afterChange();
	
	// changing methods

	@Override
	public boolean add(E e) {
		boolean result = super.add(e);
		afterChange();
		return result;
	}

	@Override
	public boolean remove(Object o) {
		boolean result = super.remove(o);
		afterChange();
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean result = super.addAll(c);
		afterChange();
		return result;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		boolean result = super.addAll(index, c);
		afterChange();
		return result;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean result = super.removeAll(c);
		afterChange();
		return result;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean result = super.retainAll(c);
		afterChange();
		return result;
	}

	@Override
	public void clear() {
		super.clear();
		afterChange();
	}

	@Override
	public E set(int index, E element) {
		E result = super.set(index, element);
		afterChange();
		return result;
	}

	@Override
	public void add(int index, E element) {
		super.add(index, element);
		afterChange();
	}

	@Override
	public E remove(int index) {
		E result = super.remove(index);
		afterChange();
		return result;
	}
	
}

package ru.indvdum.jpa.collections

/**
 * @author indvdum (gotoindvdum@gmail.com)
 * @since 12.01.2012 17:26:13
 *
 * @param <E>
 */
class EntityList<E> extends TriggeredList<E> {
	
	def ofObject
	def getter
	def setter
	
	public EntityList() {
		super()
	}
	
	public EntityList(Object ofObject, String getter, String setter) {
		this.ofObject = ofObject
		this.getter = getter
		this.setter = setter
	}
	
	@Override
	protected void afterChange() {
		if(ofObject == null || getter == null || setter == null)
			return
		each {
			if(it."${getter}"() != ofObject) {
				it."${setter}"(ofObject)
			}
		}
	}
}

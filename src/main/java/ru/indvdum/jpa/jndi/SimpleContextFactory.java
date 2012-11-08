package ru.indvdum.jpa.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 03.11.2011 16:37:21
 * 
 */
public class SimpleContextFactory implements InitialContextFactory {

	private static Context simpleContext = null;

	@Override
	public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
		if (simpleContext == null) {
			synchronized (this) {
				simpleContext = new SimpleContext(environment);
			}
		}

		return simpleContext;
	}
}
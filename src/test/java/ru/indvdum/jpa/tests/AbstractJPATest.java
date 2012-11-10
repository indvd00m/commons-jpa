package ru.indvdum.jpa.tests;

import org.junit.BeforeClass;

import ru.indvdum.jpa.dao.JPAPropertySelector;
import ru.indvdum.jpa.init.DatabaseInitializer;

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 22.12.2011 16:44:26
 * 
 */
public abstract class AbstractJPATest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		JPAPropertySelector.setSystemProperty(JPAPropertySelector.RUNTIMEENHANCEMENT, "supported");
		JPAPropertySelector.setSystemProperty(JPAPropertySelector.SHOWSQL, "true");
		JPAPropertySelector.setSystemProperty(JPAPropertySelector.SYNCHRONIZEDB, "true");
		DatabaseInitializer.init();
	}

}

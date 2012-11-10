package ru.indvdum.jpa.tests;

import org.junit.BeforeClass;

import ru.indvdum.jpa.init.DatabaseInitializer;

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 22.12.2011 16:44:26
 * 
 */
public abstract class AbstractJPATest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		DatabaseInitializer.init();
	}

}

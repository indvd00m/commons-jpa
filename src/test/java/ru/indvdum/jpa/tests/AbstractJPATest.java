package ru.indvdum.jpa.tests;

import java.util.ResourceBundle;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.BeforeClass;

import ru.indvdum.jpa.dao.PropertySelector;
import ru.indvdum.jpa.props.Props;

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 22.12.2011 16:44:26
 * 
 */
public abstract class AbstractJPATest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		if ("true".equalsIgnoreCase(System.getProperty("JPA.realDatabaseConnection"))) {
			initRealDatabase();
		} else {
			initDerby();
		}
	}

	protected static String dataSource = null;

	protected static String getDataSourceName() {
		if (dataSource != null)
			return dataSource;

		if (dataSource == null)
			dataSource = System.getProperty(Props.DATA_SOURCE_PROPERTY);
		if (dataSource == null)
			dataSource = ResourceBundle.getBundle(Props.JPADAO_PROPERTY_FILE).getString(Props.DATA_SOURCE_PROPERTY);
		if (dataSource == null)
			dataSource = "jdbc/database";
		return dataSource;
	}

	private static void initDerby() throws NamingException {
		System.setProperty("derby.stream.error.field", "java.lang.System.err");

		InitialContext context = new InitialContext();
		BasicDataSource datasource = (BasicDataSource) context.lookup(getDataSourceName());

		if (datasource == null) {
			datasource = new BasicDataSource();
			datasource.setUrl("jdbc:derby:memory:myDb;create=true");
			datasource.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
			datasource.setUsername("");
			datasource.setPassword("");
			datasource.setMaxActive(2);

			context.bind(getDataSourceName(), datasource);
		}

		PropertySelector.setSystemProperty(PropertySelector.RUNTIMEENHANCEMENT, "supported");
		PropertySelector.setSystemProperty(PropertySelector.SHOWSQL, "true");
		PropertySelector.setSystemProperty(PropertySelector.SYNCHRONIZEDB, "true");
	}

	private static void initRealDatabase() throws NamingException {
		// TODO: need realize
	}

}

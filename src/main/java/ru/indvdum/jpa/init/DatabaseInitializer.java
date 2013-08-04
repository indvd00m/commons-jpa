package ru.indvdum.jpa.init;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.indvdum.jpa.dao.JPADataAccessObject;
import ru.indvdum.jpa.props.Props;

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 10.11.2012 21:40:11
 * 
 */
public class DatabaseInitializer {

	protected static Logger log = LoggerFactory.getLogger(DatabaseInitializer.class.getSimpleName());

	protected static String dataSource = null;

	protected static String getDataSourceName() throws ConfigurationException {
		if (dataSource != null)
			return dataSource;

		if (dataSource == null)
			dataSource = System.getProperty(Props.DATA_SOURCE_PROPERTY);
		if (dataSource == null) {
			try {
				dataSource = ResourceBundle.getBundle(Props.JPADAO_PROPERTY_FILE).getString(Props.DATA_SOURCE_PROPERTY);
			} catch (MissingResourceException e) {
				log.info("Configuration file " + Props.JPADAO_PROPERTY_FILE + ".properties not found");
			}
		}
		if (dataSource == null) {
			XMLConfiguration conf = new XMLConfiguration("META-INF/persistence.xml");
			dataSource = conf.getString("persistence-unit.non-jta-data-source");
		}
		if (dataSource == null)
			dataSource = "jdbc/database";
		return dataSource;
	}

	public static void init() throws NamingException, ConfigurationException {
		String url = System.getProperty(Props.DATABASE_URL_PROPERTY);
		String driver = System.getProperty(Props.DATABASE_DRIVER_PROPERTY);
		String username = System.getProperty(Props.DATABASE_USERNAME_PROPERTY);
		String password = System.getProperty(Props.DATABASE_PASSWORD_PROPERTY);
		String maxActive = System.getProperty(Props.DATABASE_MAX_ACTIVE_PROPERTY);
		if (url != null) {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl(url);
			dataSource.setDriverClassName(driver);
			dataSource.setUsername(username);
			dataSource.setPassword(password);
			if (maxActive != null && maxActive.matches("\\d+"))
				dataSource.setMaxActive(Integer.parseInt(maxActive));

			init(dataSource);
		} else
			initDerby();
	}

	public static void init(String driver, String url, String username, String password, int maxActive)
			throws NamingException, ConfigurationException {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(url);
		dataSource.setDriverClassName(driver);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setMaxActive(maxActive);

		init(dataSource);
	}

	public static void init(BasicDataSource dataSource) throws NamingException, ConfigurationException {
		InitialContext context = new InitialContext();
		BasicDataSource existedDataSource = (BasicDataSource) context.lookup(getDataSourceName());
		if (existedDataSource == null)
			context.bind(getDataSourceName(), dataSource);
		JPADataAccessObject.initEntityManagerFactory();
	}

	public static void initDerby() throws NamingException, ConfigurationException {
		System.setProperty("derby.stream.error.field", "java.lang.System.err");

		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl("jdbc:derby:memory:myDb;create=true");
		dataSource.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
		dataSource.setUsername("");
		dataSource.setPassword("");
		dataSource.setMaxActive(2);

		init(dataSource);
	}
}

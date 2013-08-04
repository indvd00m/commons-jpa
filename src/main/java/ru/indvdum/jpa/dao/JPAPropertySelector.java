package ru.indvdum.jpa.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 03.11.2011 13:53:32
 * 
 */
public class JPAPropertySelector {
	public static final String SHOWSQL = "custom.showsql";
	public static final String SYNCHRONIZEDB = "custom.synchronize";
	public static final String SCHEMANAME = "custom.schemaname";
	public static final String DBDICTIONARY = "custom.dbdictionary";
	public static final String RUNTIMEENHANCEMENT = "custom.runtimeenhancement";

	private static Properties properties = new Properties();

	private static boolean isProperty(String propertyName) {
		return Boolean.valueOf(properties.getProperty(propertyName)).booleanValue();
	}

	private static String getSystemProperty(String propertyName) {
		return properties.getProperty(propertyName);
	}

	public static void setSystemProperty(String propertyName, String propertyValue) {
		properties.setProperty(propertyName, propertyValue);
	}

	public static Map<String, String> select() {
		Map<String, String> emfProperties = new HashMap<String, String>();

		if (isProperty(SHOWSQL)) {
			emfProperties.put("openjpa.Log", "DefaultLevel=INFO, Tool=INFO, SQL=TRACE");
			emfProperties.put("openjpa.ConnectionFactoryProperties", "PrintParameters=true, PrettyPrint=true");
		}

		if (isProperty(SYNCHRONIZEDB)) {
			emfProperties.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
		}

		if (getSystemProperty(SCHEMANAME) != null) {
			emfProperties.put("openjpa.jdbc.Schema", getSystemProperty(SCHEMANAME));
		}

		if (getSystemProperty(DBDICTIONARY) != null) {
			emfProperties.put("openjpa.jdbc.DBDictionary", getSystemProperty(DBDICTIONARY));
		}

		if (getSystemProperty(RUNTIMEENHANCEMENT) != null) {
			emfProperties.put("openjpa.RuntimeUnenhancedClasses", getSystemProperty(RUNTIMEENHANCEMENT));
		}

		return emfProperties;
	}
}

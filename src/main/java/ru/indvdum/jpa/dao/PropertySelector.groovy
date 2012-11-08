package ru.indvdum.jpa.dao

/**
 * @author indvdum (gotoindvdum[at]gmail[dot]com)
 * @since 03.11.2011 13:53:32
 *
 */
class PropertySelector {
	public static final String SHOWSQL = "custom.showsql"
	public static final String SYNCHRONIZEDB = "custom.synchronize"
	public static final String SCHEMANAME = "custom.schemaname"
	public static final String DBDICTIONARY = "custom.dbdictionary"
	public static final String RUNTIMEENHANCEMENT = "custom.runtimeenhancement"

	private static Properties properties = new Properties()

	private static boolean isProperty(String propertyName) {
		return Boolean.valueOf(properties.getProperty(propertyName)).booleanValue()
	}

	private static String getSystemProperty(String propertyName) {
		properties.getProperty(propertyName)
	}

	static void setSystemProperty(String propertyName, String propertyValue) {
		properties.setProperty(propertyName, propertyValue)
	}

	static Map select() {
		Map<String, String> emfProperties = [:]

		if(isProperty(SHOWSQL)) {
			emfProperties["openjpa.Log"] = "DefaultLevel=INFO, Tool=INFO, SQL=TRACE"
			emfProperties["openjpa.ConnectionFactoryProperties"] = "PrintParameters=true, PrettyPrint=true"
		}

		if(isProperty(SYNCHRONIZEDB)) {
			emfProperties["openjpa.jdbc.SynchronizeMappings"] = "buildSchema(ForeignKeys=true)"
		}

		if(getSystemProperty(SCHEMANAME) != null) {
			emfProperties["openjpa.jdbc.Schema"] = getSystemProperty(SCHEMANAME)
		}

		if(getSystemProperty(DBDICTIONARY)) {
			emfProperties["openjpa.jdbc.DBDictionary"] = getSystemProperty(DBDICTIONARY)
		}

		if(getSystemProperty(RUNTIMEENHANCEMENT)) {
			emfProperties["openjpa.RuntimeUnenhancedClasses"] = getSystemProperty(RUNTIMEENHANCEMENT)
		}

		return emfProperties
	}
}

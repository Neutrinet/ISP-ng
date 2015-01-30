package be.neutrinet.ispng.util;

import com.j256.ormlite.db.MysqlDatabaseType;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDBType extends MysqlDatabaseType {

    private final static String DATABASE_URL_PORTION = "mysql";
    private final static String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
    private final static String DATABASE_NAME = "MySQL";

    @Override
    public boolean isDatabaseUrlThisType(String url, String dbTypePart) {
        return DATABASE_URL_PORTION.equals(dbTypePart);
    }

    @Override
    public void loadDriver() throws SQLException {
        try {
            Driver d = (Driver) Class.forName(DRIVER_CLASS_NAME).newInstance();
            DriverManager.registerDriver(d);
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }

    @Override
    protected String getDriverClassName() {
        return DRIVER_CLASS_NAME;
    }

    @Override
    public String getDatabaseName() {
        return DATABASE_NAME;
    }

    public boolean isDatetimeFieldWidthSupported() {
        return true;
    }
}

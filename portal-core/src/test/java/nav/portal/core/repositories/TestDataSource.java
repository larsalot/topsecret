package nav.portal.core.repositories;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public class TestDataSource {

    public static DataSource create() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        Flyway.configure().dataSource(dataSource).load().migrate();
        return dataSource;
    }


}


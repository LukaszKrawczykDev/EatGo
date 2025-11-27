package pollub.eatgo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${PGHOST:}")
    private String pgHost;

    @Value("${PGPORT:5432}")
    private String pgPort;

    @Value("${PGUSER:}")
    private String pgUser;

    @Value("${PGPASSWORD:}")
    private String pgPassword;

    @Value("${PGDATABASE:}")
    private String pgDatabase;

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        
        String jdbcUrl;
        String username;
        String password;

        if (databaseUrl != null && !databaseUrl.isEmpty() && !databaseUrl.startsWith("jdbc:")) {
            try {
                URI dbUri = new URI(databaseUrl);
                username = dbUri.getUserInfo().split(":")[0];
                password = dbUri.getUserInfo().split(":")[1];
                String host = dbUri.getHost();
                int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                String dbName = dbUri.getPath().replaceFirst("/", "");
                
                jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
                
                System.out.println("DatabaseConfig: Parsed DATABASE_URL - Host: " + host + ", Port: " + port + ", Database: " + dbName);
            } catch (Exception e) {
                System.err.println("DatabaseConfig: Error parsing DATABASE_URL: " + e.getMessage());
                e.printStackTrace();
                jdbcUrl = buildJdbcUrlFromPgVars();
                username = pgUser.isEmpty() ? "postgres" : pgUser;
                password = pgPassword.isEmpty() ? "" : pgPassword;
            }
        } else if (databaseUrl != null && !databaseUrl.isEmpty() && databaseUrl.startsWith("jdbc:")) {
            jdbcUrl = databaseUrl;
            username = pgUser.isEmpty() ? "postgres" : pgUser;
            password = pgPassword.isEmpty() ? "" : pgPassword;
        } else {
            jdbcUrl = buildJdbcUrlFromPgVars();
            username = pgUser.isEmpty() ? "postgres" : pgUser;
            password = pgPassword.isEmpty() ? "" : pgPassword;
        }

        properties.setUrl(jdbcUrl);
        properties.setUsername(username);
        properties.setPassword(password);
        properties.setDriverClassName("org.postgresql.Driver");

        System.out.println("DatabaseConfig: JDBC URL: " + jdbcUrl.replaceAll(":[^:@]+@", ":****@"));
        System.out.println("DatabaseConfig: Username: " + username);

        return properties;
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    private String buildJdbcUrlFromPgVars() {
        String host = pgHost.isEmpty() ? "localhost" : pgHost;
        String port = pgPort.isEmpty() ? "5432" : pgPort;
        String db = pgDatabase.isEmpty() ? "eatgo" : pgDatabase;
        return String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
    }
}


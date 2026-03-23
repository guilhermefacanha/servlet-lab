package servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@WebServlet(value = "/db-health", loadOnStartup = 99)
public class DatabaseSupaHealthChecker extends HttpServlet {

	private static final long serialVersionUID = 2568619309288262733L;
	private static final Logger LOGGER = Logger.getLogger(DatabaseSupaHealthChecker.class.getName());

	private static final String JDBC_URL = "jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:6543/postgres";
	private static final String DB_USER = "postgres.yxvqvzzfleyhqnfrknli";

	// Replace with your real table name.
	private static final String TABLE_NAME = "profiles";
	private static final String QUERY_TEMPLATE = "SELECT count(*) FROM %s";
	private static final long CHECK_INTERVAL_MINUTES = 5;

	private final AtomicBoolean checkRunning = new AtomicBoolean(false);
	private String dbPassword;
	private ScheduledExecutorService scheduler;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		dbPassword = resolveDbPassword();
		if (dbPassword == null) {
			throw new ServletException("Missing SUPA_DB_PASSWORD. Set -DSUPA_DB_PASSWORD=... or environment variable SUPA_DB_PASSWORD for DatabaseSupaHealthChecker");
		}

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			throw new ServletException("PostgreSQL JDBC driver not found", e);
		}

		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "db-supa-health-checker");
			t.setDaemon(true);
			return t;
		});

		scheduler.scheduleAtFixedRate(this::safeRunHealthCheck, 0, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
		LOGGER.info("DatabaseSupaHealthChecker started. Health checks every " + CHECK_INTERVAL_MINUTES + " minutes.");
	}

	@Override
	public void destroy() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
		super.destroy();
	}

	private void safeRunHealthCheck() {
		try {
			runHealthCheck();
		} catch (Exception e) {
			LOGGER.warning("Database health check failed: " + e.getMessage());
		}
	}

	private void runHealthCheck() throws SQLException {
		if (!checkRunning.compareAndSet(false, true)) {
			LOGGER.info("Skipping health check because a previous execution is still running.");
			return;
		}

		try {
			String query = buildQuery(TABLE_NAME);
            LOGGER.info(" ########################################   ");
			LOGGER.info("Running DB health query: " + query);

			try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, dbPassword);
				 Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery(query)) {

				ResultSetMetaData metaData = resultSet.getMetaData();
				int rowCount = 0;

				while (resultSet.next()) {
					rowCount = resultSet.getInt(1);
				}

				LOGGER.info("Database health query completed with " + rowCount + " row(s).");
                LOGGER.info(" ########################################   ");
			}
		} finally {
			checkRunning.set(false);
		}
	}

	private String buildQuery(String tableName) {
		if (!tableName.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			throw new IllegalArgumentException("Invalid table name: " + tableName);
		}
		return String.format(QUERY_TEMPLATE, tableName);
	}

	private String resolveDbPassword() {
		String fromProperty = trimToNull(System.getProperty("SUPA_DB_PASSWORD"));
		if (fromProperty != null) {
			return fromProperty;
		}
		return trimToNull(System.getenv("SUPA_DB_PASSWORD"));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}


package com.cryptoseek;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Logger;

@WebListener
public class MySqlDriverCleanupListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(MySqlDriverCleanupListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Nothing needed here
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Deregister JDBC drivers
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                logger.info("Deregistered JDBC driver: " + driver);
            } catch (SQLException e) {
                logger.warning("Error deregistering JDBC driver: " + e.getMessage());
            }
        }
        
        // Stop the AbandonedConnectionCleanupThread
        try {
            // For MySQL Connector/J 8.0+
            Class<?> cleanupThreadClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            Object cleanupThread = cleanupThreadClass.getMethod("checkedShutdown").invoke(null);
            logger.info("MySQL AbandonedConnectionCleanupThread shutdown completed");
        } catch (Exception e) {
            logger.warning("Failed to shutdown MySQL AbandonedConnectionCleanupThread: " + e.getMessage());
        }
    }
}
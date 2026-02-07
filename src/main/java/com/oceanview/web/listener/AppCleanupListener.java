package com.oceanview.web.listener;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

@WebListener
public class AppCleanupListener implements ServletContextListener {

    // app start
    @Override
    public void contextInitialized(ServletContextEvent sce) {
       
    }

    // app stop
    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        // stop mysql cleanup thread
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
        }

        // deregister jdbc drivers
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver d = drivers.nextElement();
                try {
                    DriverManager.deregisterDriver(d);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }
}

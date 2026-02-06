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

    @Override
    public void contextInitialized(ServletContextEvent sce) {
       
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
   
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
        }

     
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

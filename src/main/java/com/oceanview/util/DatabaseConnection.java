package com.oceanview.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
	  private static DatabaseConnection instance;
	 
	    private static final String USER = "root";
	    private static final String PASSWORD = ""; 
	    private static final String URL =
	    	    "jdbc:mysql://localhost:3306/ocean_view_resort?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

	    	private DatabaseConnection() {
	    	    try { Class.forName("com.mysql.cj.jdbc.Driver"); }
	    	    catch (ClassNotFoundException e) { throw new RuntimeException(e); }
	    	}


	    public static synchronized DatabaseConnection getInstance() {
	        if (instance == null) {
	            instance = new DatabaseConnection();
	        }
	        return instance;
	    }

	    public Connection getConnection() throws SQLException {
	        return DriverManager.getConnection(URL, USER, PASSWORD);
	    }

	    
	    public static void main(String[] args) {
	        try {
	            Connection con = DatabaseConnection.getInstance().getConnection();
	            System.out.println("Connected successfully!");
	            con.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }

}

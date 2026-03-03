# Ocean View Resort – Online Room Reservation System

## Project Overview

The Ocean View Resort – Online Room Reservation System is a web-based application developed using Java Servlets, JSP, HTML, CSS, JavaScript, and MySQL.

The system replaces manual room reservation handling with a computerized solution that improves efficiency, accuracy, and data consistency. It allows administrators and staff members to manage rooms, guests, reservations, payments, and invoices through a centralized platform.

This project was developed as part of the Advanced Programming (CIS6003) module.

---

## Features

The system provides the following core functionalities:

- User Authentication (Admin / Staff / Guest)
- Room Management (Add, Update, Delete, View)
- Guest Management
- Reservation Management
- Reservation Payment Handling
- Invoice Generation
- System Settings Configuration
- Role-Based Access Control

---

## Technologies Used

- Backend: Java V17 (Servlets, JDBC)
- Frontend: HTML, CSS, JavaScript, JSP
- Database: MySQL
- Server: Apache Tomcat V9.0
- Build Tool: Maven
- Version Control: Git & GitHub

---

## System Architecture

The application follows a layered three-tier architecture:

### 1. Presentation Layer
- HTML
- CSS
- JavaScript

### 2. Business Logic Layer
- Service Interfaces
- Service Implementations

### 3. Data Access Layer
- DAO Interfaces
- DAO Implementations

Database connectivity is managed using JDBC and a Singleton `DatabaseConnection` utility class.

---

## Project Structure

```
OceanViewReservationSystem/
│
├── src/main/java/
│   ├── controller/        (Servlet Controllers)
│   ├── service/           (Service Interfaces)
│   ├── service/impl/      (Service Implementations)
│   ├── dao/               (DAO Interfaces)
│   ├── dao/impl/          (DAO Implementations)
│   ├── model/             (Entity Classes)
│   ├── util/              (Utility Classes)
│   └── filter/            (Authentication Filters)
│
├── src/main/webapp/
│   ├── css/
│   ├── js/
│   ├── views/
│   └── WEB-INF/
│
└── pom.xml
```

---

## Database Design

Main database tables:

- users
- guests
- rooms
- reservations
- reservation_payments
- settings

Primary keys and foreign key relationships are implemented to maintain referential integrity.

---

## Design Patterns Used

- DAO Pattern – Separates persistence logic from business logic
- Singleton Pattern – Used for DatabaseConnection management
- MVC Pattern – Structured separation of concerns

---

## How to Clone the Repository

Clone the repository using Git:

```bash
git clone https://github.com/mjaylk/OceanView.git
```

Navigate into the project folder:

```bash
cd OceanView
```

---

## How to Run the Project

### Step 1: Import the Project

1. Open Eclipse IDE  
2. Go to File → Import  
3. Select Existing Maven Project  
4. Choose the cloned project folder  
5. Click Finish  

### Step 2: Setup the Database

Open MySQL and create a new database:

```sql
CREATE DATABASE oceanview_resort;
```

Import the provided SQL file if available.

### Step 3: Configure Database Connection

Open:

```
src/main/java/util/DatabaseConnection.java
```

Update the database credentials:

```java
private static final String URL = "jdbc:mysql://localhost:3306/oceanview_resort";
private static final String USER = "root";
private static final String PASSWORD = "your_password";
```

### Step 4: Configure Apache Tomcat

1. Install Apache Tomcat 9  
2. Add Tomcat Server in Eclipse  
3. Deploy the project to the server  

### Step 5: Run the Application

Start the Tomcat server and open:

```
http://localhost:8080/OceanViewReservationSystem
```

---

## Learning Outcomes

This project demonstrates:

- Object-Oriented Programming Principles
- JDBC Database Integration
- Layered Architecture Implementation
- MVC Pattern in Web Applications
- Role-Based Authentication and Authorization
- Maven Project Structure
- Version Control using Git

---

## Author

Developed by:

Vishwa  
Advanced Programming (CIS6003)  
ICBT Campus
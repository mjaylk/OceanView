# Ocean View Resort – Online Room Reservation System

## Project Overview

The Ocean View Resort Online Room Reservation System is a web-based application developed using Java Servlets, HTML, CSS, JavaScript, and MySQL.  

The system was designed to replace manual reservation handling with a computerized solution that improves accuracy, efficiency, and data consistency.

This project was developed as part of the **Advanced Programming (CIS6003)** module.

---

## Features

The system provides the following functionalities:

- User Authentication (Admin / Staff / Guest)
- Room Management
- Guest Management
- Reservation Management
- Reservation Payment Handling
- Invoice Generation
- System Settings Management

---

## Technologies Used

- **Backend:** Java V17 (Servlets, JDBC)
- **Frontend:** HTML, CSS, JavaScript
- **Database:** MySQL
- **Server:** Apache Tomcat V9.0
- **Version Control:** Git & GitHub

---

## System Architecture

The application follows a three-tier architecture:

1. **Presentation Layer** – HTML, CSS, JavaScript
2. **Business Logic Layer** – Service Classes
3. **Data Access Layer** – DAO Classes

Database interactions are handled through DAO classes using JDBC.

---

## Database Design

The database consists of the following main tables:

- users
- guests
- rooms
- reservations
- reservation_payments
- settings

Primary keys and foreign key relationships were implemented to ensure data integrity.

---

## Design Patterns Used

The system implements the following design patterns:

- **DAO Pattern** – Separation of persistence logic
- **Singleton Pattern** – DatabaseConnection class

---

## How to Run the Project

1. Import the project into Eclipse
2. Configure Apache Tomcat Server
3. Setup MySQL Database
4. Update database credentials in `DatabaseConnection.java`
5. Deploy and run the application

---

## Learning Outcomes

This project demonstrates:

- Object-Oriented Programming Concepts
- Database Integration (JDBC)
- Web-Based Application Development
- UML Modelling
- Version Control Practices

---

## Author

Developed by:

**Vishwa**

Advanced Programming Module  
ICBT Campus

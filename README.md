💰 **Finance Tracker System**

A Java-based desktop Finance Tracker application that allows users to manage their personal finances through secure authentication, transaction tracking, and automated financial reporting.
Features
🔐 User Registration & Login System
👤 Session Management for secure access
💳 Financial Tracking and record management
🏦 ATM-style dashboard interface
🗄️ Database integration for persistent storage
📄 PDF report generation (statements & summaries)
📊 Financial insights and tracking tools
**Technology used**
Java (Core)
Java Swing (GUI)
JDBC (Database Connectivity)
PDF Generation Library
MySQL / Database (if applicable)
IntelliJ IDEA (Development Environment)
**project folder**
Finance Tracker/
│
├── src/
│   ├── Main.java
│   ├── LoginForm.java
│   ├── RegisterForm.java
│   ├── Dashboard.java
│   ├── ATMDashboard.java
│   ├── DatabaseConnection.java
│   ├── Session.java
│   ├── PDFTest.java
│   ├── ConfigLoader.java
│   └── DaveSavingScheme2.java
│
├── .gitignore
└── Finance Tracker.iml

** Project Purpose**

This project simulates a real-world personal finance management system. It was built to strengthen skills in:

Object-Oriented Programming (OOP)
GUI development using Java Swing
Database integration using JDBC
File handling and PDF generation

**Future Improvements**
Web version using Spring Boot 🌐
Mobile app version 📱
Advanced analytics dashboard 📊
Cloud database integration ☁️
Enhanced security (encryption & hashing)
**Author**

David Odhiambo
Java Developer | Aspiring Software Engineer
GitHub: DavidOdhiambo234
Software design for real-world applications
**If you like this project**

Give it a ⭐ on GitHub and feel free to fork or contribute!

## Email verification (Brevo)

Desktop registration verifies the supplied email address with a one-time code before activating the account. Configure Brevo with either environment variables or these properties in `config.properties`:

```properties
brevo.api.key=YOUR_BREVO_API_KEY
brevo.from.email=YOUR_VERIFIED_SENDER_EMAIL
brevo.from.name=Supreme Money Coach
```

Environment variables `BREVO_API_KEY` and `MAIL_FROM_EMAIL` take precedence; `MAIL_FROM_NAME` is optional. Verify the sender address in Brevo before sending. Verification codes expire after 10 minutes.
The legacy SendGrid settings are not used by the email service.

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*, java.util.Properties, javax.mail.*, javax.mail.internet.*" %>

<%!
    // Constants
    final String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    final String DB_USER = "root";
    final String DB_PASSWORD = "Amol@1809";

    final String EMAIL_FROM = "kaam.dhanda407@gmail.com";
    final String EMAIL_PASSWORD = "qmluidtqxzckkvmm";

    private boolean userExists(Connection conn, String username, String email) throws SQLException {
        String sql = "SELECT id FROM users WHERE full_name = ? OR email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void sendAdminNotificationEmail(String username, String email) {
        String subject = "New User Registration Requires Approval";
        String body = "A new user has registered and requires approval:\n\n"
                    + "Name: " + username + "\n"
                    + "Email: " + email + "\n\n"
                    + "Please log in to the admin dashboard to review this registration.";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("admin@yourdomain.com")); // Change to admin email
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            System.out.println("Admin notification email sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Failed to send admin notification email.");
        }
    }

    private void sendPendingApprovalEmail(String to, String username) {
        String subject = "Your Registration is Pending Approval";
        String body = "Dear " + username + ",\n\n"
                    + "Thank you for registering on our platform. Your account is currently pending approval by an administrator.\n\n"
                    + "You will receive another email once your account has been approved and you can log in.\n\n"
                    + "Regards,\nThe Admin Team";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            System.out.println("Pending approval email sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Failed to send pending approval email.");
        }
    }
%>

<%
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Check if user already exists
            if (userExists(conn, username, email)) {
                response.sendRedirect("user.html?error=Username+or+email+already+exists");
                return;
            }

            // Insert new user with pending approval status
            String sql = "INSERT INTO users (full_name, password, email, approval_status) VALUES (?, ?, ?, 'pending')";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, email);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    // Get the generated user ID
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    int userId = -1;
                    if (generatedKeys.next()) {
                        userId = generatedKeys.getInt(1);
                    }
                    
                    // Insert into registration_requests table (optional)
                    String requestSql = "INSERT INTO registration_requests (user_id) VALUES (?)";
                    try (PreparedStatement requestStmt = conn.prepareStatement(requestSql)) {
                        requestStmt.setInt(1, userId);
                        requestStmt.executeUpdate();
                    }

                    // Send emails
                    sendAdminNotificationEmail(username, email);
                    sendPendingApprovalEmail(email, username);

                    response.sendRedirect("Userlogin.html?success=Registration+successful!+Account+pending+approval");
                } else {
                    response.sendRedirect("user.html?error=Registration+failed");
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            response.sendRedirect("user.html?error=Database+error");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
%>
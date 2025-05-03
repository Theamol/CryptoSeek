<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*, java.util.Properties, javax.mail.*, javax.mail.internet.*" %>

<%!
    // Move constants to declaration section so they're accessible to all methods
    final String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    final String DB_USER = "root";
    final String DB_PASSWORD = "Amol@1809";

    final String EMAIL_FROM = "kaam.dhanda407@gmail.com";
    final String EMAIL_PASSWORD = "qmluidtqxzckkvmm";

    private boolean userExists(Connection conn, String username, String email) throws SQLException {
        String sql = "SELECT id FROM admins WHERE name = ? OR email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void sendWelcomeEmail(String to, String username) {
        String subject = "Welcome to Cryproseek!";
        String body = "Dear " + username + ",\n\nThank you for registering on Cryptoseek.\n\n"
                    + "we are glad to have you with us.\n\n"
                    + "Regards,\nCryptoSeek Team";

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
            System.out.println("Welcome email sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Failed to send welcome email.");
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
                response.sendRedirect("AdminLogin.html?error=Username+or+email+already+exists");
                return;
            }

            // Insert new user with plain text password
            String sql = "INSERT INTO admins (name, password, email) VALUES (?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, email);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    // Send welcome email
                    sendWelcomeEmail(email, username);

                    response.sendRedirect("AdminLogin.html?success=Registration+successful!+Account+pending+approval");
                } else {
                    response.sendRedirect("AdminSignup.html?error=Registration+failed");
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            response.sendRedirect("AdminSignup.html?error=Database+error");
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
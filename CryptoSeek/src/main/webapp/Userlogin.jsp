<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    // Database credentials
    String dbUrl = "jdbc:mysql://localhost:3306/file_encryption";
    String dbUsername = "root";
    String dbPassword = "Amol@1809";

    // Get login form data
    String email = request.getParameter("email");
    String password = request.getParameter("password");

    // Response defaults
    boolean loginSuccess = false;
    String errorMessage = "";
    String redirectPage = "Userlogin.html";

    // Input validation
    if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
        errorMessage = "Email and password are required.";
    } else {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Load driver
            Class.forName("com.mysql.jdbc.Driver");

            // Connect to DB
            conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);

            // Check if user exists and is approved
            String sql = "SELECT id, email, password, approval_status FROM users WHERE email = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String approvalStatus = rs.getString("approval_status");

                if (!"approved".equals(approvalStatus)) {
                    errorMessage = "Your account is not yet approved. Please wait for administrator approval.";
                } else if (password.equals(storedPassword)) {
                    loginSuccess = true;

                    // Store email and id in session
                    session.setAttribute("userId", rs.getInt("id"));
                    session.setAttribute("userEmail", rs.getString("email"));
                    
                    // Set session timeout to 20 minutes (in seconds)
                    session.setMaxInactiveInterval(15 * 60);
                    
                    // Regenerate session ID to prevent fixation
                    session.invalidate();
                    session = request.getSession(true);
                    session.setAttribute("userId", rs.getInt("id"));
                    session.setAttribute("userEmail", rs.getString("email"));
                    session.setMaxInactiveInterval(15* 60);

                    // Update last login time
                    PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE users SET last_login = NOW() WHERE id = ?");
                    updateStmt.setInt(1, rs.getInt("id"));
                    updateStmt.executeUpdate();
                    updateStmt.close();

                    redirectPage = "Dashboard.html"; // Redirect to dashboard on success
                } else {
                    errorMessage = "Invalid password.";
                }
            } else {
                errorMessage = "No account found with this email.";
            }
        } catch (Exception e) {
            errorMessage = "Error: " + e.getMessage();
            e.printStackTrace();
        } finally {
            // Cleanup
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    // Redirect or show error
    if (loginSuccess) {
        response.sendRedirect(redirectPage);
    } else {
        response.sendRedirect("Userlogin.html?error=" + java.net.URLEncoder.encode(errorMessage, "UTF-8"));
    }
%>
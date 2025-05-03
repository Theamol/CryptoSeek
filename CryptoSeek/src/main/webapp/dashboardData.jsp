<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.sql.*" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<%
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setHeader("Expires", "0"); // Proxies

    String fullName = "";
    int totalFiles = 0;
    int userFiles = 0;
    String userEmail = null;
    
    // Get the session and check for user login
    HttpSession userSession = request.getSession(false);
    if (userSession != null) {
        userEmail = (String) userSession.getAttribute("userEmail");
    }

    if (userEmail == null) {
        // Return JSON response for unauthorized access
        out.print("{\"error\":\"User not logged in\",\"redirect\":\"Userlogin.html\"}");
        return;
    }

    // Database connection details
    String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    String DB_USER = "root";
    String DB_PASS = "Amol@1809";

    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

        // Get user's full name
        String nameQuery = "SELECT full_name FROM users WHERE email = ?";
        PreparedStatement nameStmt = conn.prepareStatement(nameQuery);
        nameStmt.setString(1, userEmail);
        ResultSet nameRs = nameStmt.executeQuery();
        if (nameRs.next()) {
            fullName = nameRs.getString("full_name");
        }

        // Get total files count
        String totalQuery = "SELECT COUNT(*) AS total FROM files";
        PreparedStatement totalStmt = conn.prepareStatement(totalQuery);
        ResultSet totalRs = totalStmt.executeQuery();
        if (totalRs.next()) {
            totalFiles = totalRs.getInt("total");
        }

        // Get user's files count
        String userQuery = "SELECT COUNT(*) AS user_count FROM files WHERE email = ?";
        PreparedStatement userStmt = conn.prepareStatement(userQuery);
        userStmt.setString(1, userEmail);
        ResultSet userRs = userStmt.executeQuery();
        if (userRs.next()) {
            userFiles = userRs.getInt("user_count");
        }

        conn.close();
        
        // Return the data as JSON
        out.print(String.format(
            "{\"fullName\":\"%s\",\"totalFiles\":%d,\"userFiles\":%d}",
            fullName != null ? fullName.replace("\"", "\\\"") : "",
            totalFiles,
            userFiles
        ));
    } catch (Exception e) {
        e.printStackTrace();
        out.print("{\"error\":\"Database error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
    }
%>
package com.cryptoseek;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

@WebServlet("/DashboardServlet")
public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // Database configuration - update with your details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Amol@1809";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        JSONObject jsonResponse = new JSONObject();
        
        if (session == null || session.getAttribute("userEmail") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.put("error", "Not logged in");
            response.getWriter().write(jsonResponse.toString());
            return;
        }
        
        String userEmail = (String) session.getAttribute("userEmail");
        Connection conn = null;
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            
            // Get user's full name
            String fullName = getUserFullName(conn, userEmail);
            
            // Get total files count
            int totalFiles = getTotalFilesCount(conn);
            
            // Get user's files count
            int userFiles = getUserFilesCount(conn, userEmail);
            
            // Prepare JSON response
            jsonResponse.put("fullName", fullName);
            jsonResponse.put("totalFiles", totalFiles);
            jsonResponse.put("userFiles", userFiles);
            
            response.getWriter().write(jsonResponse.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("error", "Database error: " + e.getMessage());
            response.getWriter().write(jsonResponse.toString());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception e) { /* Ignored */ }
            }
        }
    }
    
    private String getUserFullName(Connection conn, String email) throws Exception {
        String query = "SELECT full_name FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("full_name") : "";
        }
    }
    
    private int getTotalFilesCount(Connection conn) throws Exception {
        String query = "SELECT COUNT(*) AS total FROM files";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
        }
    }
    
    private int getUserFilesCount(Connection conn, String email) throws Exception {
        String query = "SELECT COUNT(*) AS user_count FROM files WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("user_count") : 0;
        }
    }
}
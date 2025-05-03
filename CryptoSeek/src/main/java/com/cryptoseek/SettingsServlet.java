package com.cryptoseek;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

@WebServlet("/SettingsServlet")
public class SettingsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Amol@1809";
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false); // Get existing session, don't create new
        if (session == null) {
            sendErrorResponse(response, "Session expired. Please login again.");
            return;
        }
        
        String email = (String) session.getAttribute("userEmail");
        if (email == null || email.isEmpty()) {
            sendErrorResponse(response, "User not authenticated. Please login again.");
            return;
        }
        
        String action = request.getParameter("action");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            if ("updateProfile".equals(action)) {
                updateProfile(conn, email, request, response);
            } else if ("changePassword".equals(action)) {
                changePassword(conn, email, request, response);
            } else {
                sendErrorResponse(response, "Invalid action");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, "Database error: " + e.getMessage());
        }
    }
    
    private void updateProfile(Connection conn, String email, HttpServletRequest request, 
            HttpServletResponse response) throws IOException, SQLException {
        
        String newName = request.getParameter("name");
        
        if (newName == null || newName.trim().isEmpty()) {
            sendErrorResponse(response, "Name cannot be empty");
            return;
        }
        
        String sql = "UPDATE users SET full_name = ? WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName.trim());
            pstmt.setString(2, email);
            
            int rowsAffected = pstmt.executeUpdate();
            
            JSONObject jsonResponse = new JSONObject();
            if (rowsAffected > 0) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", "Profile updated successfully");
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "No changes made to profile");
            }
            response.getWriter().write(jsonResponse.toString());
        }
    }
    
    private void changePassword(Connection conn, String email, HttpServletRequest request, 
            HttpServletResponse response) throws IOException, SQLException {
        
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        
        if (currentPassword == null || newPassword == null || 
            currentPassword.isEmpty() || newPassword.isEmpty()) {
            sendErrorResponse(response, "Both current and new password are required");
            return;
        }
        
        // First verify current password
        String verifySql = "SELECT password FROM users WHERE email = ?";
        try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
            verifyStmt.setString(1, email);
            ResultSet rs = verifyStmt.executeQuery();
            
            JSONObject jsonResponse = new JSONObject();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                
                // Compare passwords (in production, use password hashing)
                if (!storedPassword.equals(currentPassword)) {
                    jsonResponse.put("success", false);
                    jsonResponse.put("message", "Current password is incorrect");
                    response.getWriter().write(jsonResponse.toString());
                    return;
                }
                
                // Update password
                String updateSql = "UPDATE users SET password = ? WHERE email = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, newPassword);
                    updateStmt.setString(2, email);
                    
                    int rowsAffected = updateStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        jsonResponse.put("success", true);
                        jsonResponse.put("message", "Password changed successfully");
                    } else {
                        jsonResponse.put("success", false);
                        jsonResponse.put("message", "Failed to change password");
                    }
                    response.getWriter().write(jsonResponse.toString());
                }
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "User not found");
                response.getWriter().write(jsonResponse.toString());
            }
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("success", false);
        jsonResponse.put("message", message);
        response.getWriter().write(jsonResponse.toString());
    }
}
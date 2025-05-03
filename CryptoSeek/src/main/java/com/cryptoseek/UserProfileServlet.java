package com.cryptoseek;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "UserProfileServlet", value = "/UserProfileServlet")
public class UserProfileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Amol@1809";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("username");
        
        if (username == null) {
            response.sendRedirect("Userlogin.html");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT full_name, email FROM users WHERE full_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    request.setAttribute("", rs.getString("full_name"));
                    request.setAttribute("email", rs.getString("email"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error retrieving user data");
        }
        
        request.getRequestDispatcher("profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("username");
        String currentUsername = request.getParameter("currentUsername");
        String newUsername = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (username == null || !username.equals(currentUsername)) {
            response.sendRedirect("login.jsp");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql;
            PreparedStatement stmt;
            
            if (password != null && !password.isEmpty()) {
                sql = "UPDATE users SET username = ?, email = ?, password = ? WHERE username = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, newUsername);
                stmt.setString(2, email);
                stmt.setString(3, password); // In production, you should hash this password
                stmt.setString(4, currentUsername);
            } else {
                sql = "UPDATE users SET username = ?, email = ? WHERE username = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, newUsername);
                stmt.setString(2, email);
                stmt.setString(3, currentUsername);
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                session.setAttribute("username", newUsername);
                request.setAttribute("success", "Profile updated successfully!");
            } else {
                request.setAttribute("error", "Failed to update profile");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error updating profile: " + e.getMessage());
        }
        
        request.getRequestDispatcher("profile.jsp").forward(request, response);
    }
}
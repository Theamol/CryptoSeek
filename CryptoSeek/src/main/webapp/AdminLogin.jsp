<%@ page import="java.sql.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // Database credentials
        String dbUrl = "jdbc:mysql://localhost:3306/file_encryption";
        String dbUsername = "root";
        String dbPassword = "Amol@1809";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);

            String sql = "SELECT id, name, email FROM admins WHERE email = ? AND password = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Admin authenticated
                session.setAttribute("adminId", rs.getInt("id"));
                session.setAttribute("adminName", rs.getString("name"));
                session.setAttribute("adminEmail", rs.getString("email"));
                
                response.sendRedirect("AdminDashboard.jsp");
                return;
            } else {
                response.sendRedirect("AdminLogin.html?error=Invalid+credentials");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("AdminLogin.html?error=Database+error");
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }
    }
%>
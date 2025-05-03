<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.mail.*" %>
<%@ page import="javax.mail.internet.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%!
    // Email sending method
    private void sendApprovalNotificationEmail(String to, String username) {
        String subject = "Your Account Has Been Approved";
        String body = "Dear " + username + ",\n\n"
                    + "Your account registration has been approved by an administrator.\n\n"
                    + "You can now log in to your account using the credentials you provided during registration.\n\n"
                    + "Regards,\nThe Admin Team";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("kaam.dhanda407@gmail.com", "qmluidtqxzckkvmm");
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("kaam.dhanda407@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            System.out.println("Approval notification email sent successfully.");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Failed to send approval notification email.");
        }
    }

    // Database connection method
    private Connection getConnection() throws SQLException, ClassNotFoundException {
        String dbUrl = "jdbc:mysql://localhost:3306/file_encryption";
        String dbUsername = "root";
        String dbPassword = "Amol@1809";
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }
%>

<%
    // Check if admin is logged in
    if (session.getAttribute("adminId") == null) {
        response.sendRedirect("AdminLogin.jsp");
        return;
    }

    // Get admin details for profile
    Map<String, Object> adminDetails = new HashMap<>();
    try (Connection conn = getConnection()) {
        String sql = "SELECT * FROM admins WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, (Integer) session.getAttribute("adminId"));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                adminDetails.put("id", rs.getInt("id"));
                adminDetails.put("name", rs.getString("name"));
                adminDetails.put("email", rs.getString("email"));
                adminDetails.put("name", rs.getString("name"));
                adminDetails.put("created_at", rs.getTimestamp("created_at"));
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    // Process approval/rejection if form submitted
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String action = request.getParameter("action");
        if (action != null && (action.equals("approve") || action.equals("reject"))) {
            int userId = Integer.parseInt(request.getParameter("userId"));
            int adminId = (Integer) session.getAttribute("adminId");

            try (Connection conn = getConnection()) {
                if ("approve".equals(action)) {
                    String sql = "UPDATE users SET approval_status = 'approved', approved_by = ?, approval_date = NOW() WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, adminId);
                        pstmt.setInt(2, userId);
                        pstmt.executeUpdate();
                    }

                    // Get user email to send approval notification
                    String emailSql = "SELECT email, full_name FROM users WHERE id = ?";
                    try (PreparedStatement emailStmt = conn.prepareStatement(emailSql)) {
                        emailStmt.setInt(1, userId);
                        ResultSet rs = emailStmt.executeQuery();
                        if (rs.next()) {
                            sendApprovalNotificationEmail(rs.getString("email"), rs.getString("full_name"));
                        }
                    }
                } else if ("reject".equals(action)) {
                    String sql = "UPDATE users SET approval_status = 'rejected' WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setInt(1, userId);
                        pstmt.executeUpdate();
                    }
                }

                // Also update registration_requests table if using it
                String requestSql = "UPDATE registration_requests SET status = ?, reviewed_by = ? NOW() WHERE user_id = ?";
                try (PreparedStatement requestStmt = conn.prepareStatement(requestSql)) {
                    requestStmt.setString(1, action + "d"); // "approved" or "rejected"
                    requestStmt.setInt(2, adminId);
                    requestStmt.setInt(3, userId);
                    requestStmt.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Search functionality
    String searchQuery = request.getParameter("search");
    List<Map<String, Object>> pendingUsers = new ArrayList<>();
    try (Connection conn = getConnection()) {
        String sql = "SELECT id, full_name, email, created_at FROM users WHERE approval_status = 'pending' ";
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql += "AND (full_name LIKE ? OR email LIKE ?) ";
        }
        sql += "ORDER BY created_at";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                stmt.setString(1, "%" + searchQuery + "%");
                stmt.setString(2, "%" + searchQuery + "%");
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("full_name", rs.getString("full_name"));
                user.put("email", rs.getString("email"));
                user.put("created_at", rs.getTimestamp("created_at"));
                pendingUsers.add(user);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - Pending Approvals</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <style>
        :root {
            --primary-color: #4a6bff;
            --secondary-color: #f8f9fa;
            --dark-color: #343a40;
            --light-color: #ffffff;
            --danger-color: #dc3545;
            --success-color: #28a745;
            --warning-color: #ffc107;
            --border-radius: 5px;
            --box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }

        body {
            background-color: #f5f5f5;
            color: #333;
        }

        .admin-container {
            display: flex;
            min-height: 100vh;
        }

        .sidebar {
            width: 250px;
            background-color: var(--dark-color);
            color: var(--light-color);
            padding: 20px 0;
            transition: all 0.3s;
            position: fixed;
            height: 100vh;
            z-index: 1000;
        }

        .sidebar h2 {
            text-align: center;
            margin-bottom: 30px;
            padding-bottom: 10px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }

        .sidebar ul {
            list-style: none;
        }

        .sidebar ul li {
            margin-bottom: 5px;
        }

        .sidebar ul li a {
            display: block;
            color: var(--light-color);
            padding: 10px 20px;
            text-decoration: none;
            transition: all 0.3s;
            border-left: 3px solid transparent;
        }

        .sidebar ul li a:hover {
            background-color: rgba(255, 255, 255, 0.1);
            border-left: 3px solid var(--primary-color);
        }

        .sidebar ul li a i {
            margin-right: 10px;
            width: 20px;
            text-align: center;
        }

        .sidebar ul li.active a {
            background-color: rgba(255, 255, 255, 0.1);
            border-left: 3px solid var(--primary-color);
        }

        .main-content {
            flex: 1;
            margin-left: 250px;
            padding: 20px;
            transition: all 0.3s;
        }

        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 20px;
            border-bottom: 1px solid #ddd;
            position: sticky;
            top: 0;
            background-color: var(--light-color);
            z-index: 100;
            padding: 20px;
            box-shadow: var(--box-shadow);
        }

        .header h1 {
            font-size: 24px;
            color: var(--dark-color);
        }

        .search-bar {
            display: flex;
            align-items: center;
            background-color: var(--secondary-color);
            border-radius: var(--border-radius);
            padding: 8px 15px;
            width: 300px;
        }

        .search-bar input {
            border: none;
            background: transparent;
            outline: none;
            width: 100%;
            padding: 5px;
        }

        .search-bar i {
            color: #777;
            margin-right: 10px;
        }

        .user-profile {
            display: flex;
            align-items: center;
            cursor: pointer;
            position: relative;
        }

        .user-profile img {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            margin-right: 10px;
            object-fit: cover;
        }

        .profile-dropdown {
            position: absolute;
            top: 50px;
            right: 0;
            background-color: var(--light-color);
            border-radius: var(--border-radius);
            box-shadow: var(--box-shadow);
            width: 200px;
            padding: 10px 0;
            display: none;
            z-index: 100;
        }

        .profile-dropdown.active {
            display: block;
        }

        .profile-dropdown a {
            display: block;
            padding: 10px 20px;
            color: var(--dark-color);
            text-decoration: none;
            transition: all 0.3s;
        }

        .profile-dropdown a:hover {
            background-color: var(--secondary-color);
        }

        .profile-dropdown a i {
            margin-right: 10px;
            width: 20px;
            text-align: center;
        }

        .approval-list {
            background-color: var(--light-color);
            border-radius: var(--border-radius);
            box-shadow: var(--box-shadow);
            padding: 20px;
            margin-top: 20px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        table th, table td {
            padding: 12px 15px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }

        table th {
            background-color: var(--secondary-color);
            font-weight: 600;
        }

        table tr:hover {
            background-color: rgba(0, 0, 0, 0.02);
        }

        .actions {
            display: flex;
            gap: 10px;
        }

        .btn-approve, .btn-reject {
            padding: 5px 10px;
            border: none;
            border-radius: var(--border-radius);
            cursor: pointer;
            font-weight: 500;
            transition: all 0.3s;
        }

        .btn-approve {
            background-color: var(--success-color);
            color: var(--light-color);
        }

        .btn-reject {
            background-color: var(--danger-color);
            color: var(--light-color);
        }

        .btn-approve:hover {
            background-color: #218838;
        }

        .btn-reject:hover {
            background-color: #c82333;
        }

        .no-records {
            text-align: center;
            padding: 20px;
            color: #777;
        }

        /* Responsive styles */
        @media (max-width: 992px) {
            .sidebar {
                width: 70px;
                overflow: hidden;
            }
            
            .sidebar h2, .sidebar ul li a span {
                display: none;
            }
            
            .sidebar ul li a {
                text-align: center;
                padding: 15px 5px;
            }
            
            .sidebar ul li a i {
                margin-right: 0;
                font-size: 1.2rem;
            }
            
            .main-content {
                margin-left: 70px;
            }
        }

        @media (max-width: 768px) {
            .header {
                flex-direction: column;
                align-items: flex-start;
            }
            
            .search-bar {
                width: 100%;
                margin: 10px 0;
            }
            
            table {
                display: block;
                overflow-x: auto;
            }
            
            .actions {
                flex-direction: column;
                gap: 5px;
            }
        }

        @media (max-width: 576px) {
            .sidebar {
                width: 100%;
                height: auto;
                position: relative;
                padding: 34px;
            }
            
            .sidebar ul {
                display: flex;
                flex-wrap: wrap;
                justify-content: center;
            }
            
            .sidebar ul li {
                margin: 5px;
            }
            
            .sidebar ul li a {
                padding: 10px;
                border-radius: var(--border-radius);
                border-left: none;
            }
            
            .sidebar ul li.active a {
                border-left: none;
                background-color: var(--primary-color);
            }
            
            .main-content {
                margin-left: 0;
                margin-top: 70px;
            }
        }
    </style>
</head>
<body>
    <div class="admin-container">
        <div class="sidebar">
            <h2>Admin Dashboard</h2>
            <ul>
                <li class="active"><a href="AdminDashboard.jsp"><i class="fas fa-user-check"></i> <span>Pending Approvals</span></a></li>
                <li><a href="AdminUsers.jsp"><i class="fas fa-users"></i> <span>User Management</span></a></li>
                <li><a href="AdminFiles.jsp"><i class="fas fa-file"></i> <span>File Management</span></a></li>
                <li><a href="AdminProfile.jsp"><i class="fas fa-user-cog"></i> <span>Profile</span></a></li>
                <li><a href="AdminLogout.jsp"><i class="fas fa-sign-out-alt"></i> <span>Logout</span></a></li>
            </ul>
        </div>
        
        <div class="main-content">
            <div class="header">
                <h1>Pending User Approvals</h1>
                <div class="search-bar">
                    <i class="fas fa-search"></i>
                    <form method="GET" action="AdminDashboard.jsp">
                        <input type="text" name="search" placeholder="Search users..." value="<%= searchQuery != null ? searchQuery : "" %>">
                    </form>
                </div>
                <div class="user-profile" id="profileDropdown">
                    <img src="https://ui-avatars.com/api/?name=<%= adminDetails.get("name") != null ? adminDetails.get("name").toString().replace(" ", "+") : "Admin" %>&background=4a6bff&color=fff" alt="Profile">
                    <span><%= adminDetails.get("name") != null ? adminDetails.get("name") : "Admin" %></span>
                    <div class="profile-dropdown" id="dropdownMenu">
                        <a href="AdminProfile.jsp"><i class="fas fa-user"></i> Profile</a>
                        <a href="AdminSettings.jsp"><i class="fas fa-cog"></i> Settings</a>
                        <a href="AdminLogout.jsp"><i class="fas fa-sign-out-alt"></i> Logout</a>
                    </div>
                </div>
            </div>
            
            <div class="approval-list">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Registration Date</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% if (pendingUsers.isEmpty()) { %>
                        <tr>
                            <td colspan="5" class="no-records">No pending approvals at this time.</td>
                        </tr>
                        <% } else { 
                            for (Map<String, Object> user : pendingUsers) { %>
                        <tr>
                            <td><%= user.get("id") %></td>
                            <td><%= user.get("full_name") %></td>
                            <td><%= user.get("email") %></td>
                            <td><%= user.get("created_at") %></td>
                            <td class="actions">
                                <form method="POST" style="display: inline;">
                                    <input type="hidden" name="userId" value="<%= user.get("id") %>">
                                    <button type="submit" name="action" value="approve" class="btn-approve">Approve</button>
                                </form>
                                <form method="POST" style="display: inline;">
                                    <input type="hidden" name="userId" value="<%= user.get("id") %>">
                                    <button type="submit" name="action" value="reject" class="btn-reject">Reject</button>
                                </form>
                            </td>
                        </tr>
                        <% } 
                        } %>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script>
        // Profile dropdown toggle
        document.getElementById('profileDropdown').addEventListener('click', function() {
            document.getElementById('dropdownMenu').classList.toggle('active');
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', function(event) {
            const profileDropdown = document.getElementById('profileDropdown');
            const dropdownMenu = document.getElementById('dropdownMenu');
            
            if (!profileDropdown.contains(event.target)) {
                dropdownMenu.classList.remove('active');
            }
        });

        // Auto submit search form when typing stops
        const searchInput = document.querySelector('.search-bar input');
        let searchTimeout;
        
        searchInput.addEventListener('input', function() {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                this.form.submit();
            }, 500);
        });
    </script>
</body>
</html>
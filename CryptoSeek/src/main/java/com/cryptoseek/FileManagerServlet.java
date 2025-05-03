package com.cryptoseek;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.*;
import java.util.Base64;
import javax.mail.*;
import javax.mail.internet.*;

@WebServlet("/FileManagerServlet")
public class FileManagerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Amol@1809";
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16; // AES block size for IV
    
    // Email configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USER = "kaam.dhanda407@gmail.com";
    private static final String SMTP_PASSWORD = "qmluidtqxzckkvmm";
    private static final String EMAIL_FROM = "kaam.dhanda407@gmail.com";
    private static final String EMAIL_SUBJECT = "CryptoSeek - Your Decryption Key";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load MySQL JDBC driver", e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("list".equalsIgnoreCase(action)) {
            listFiles(response);
        } else if ("download".equalsIgnoreCase(action)) {
            String fileName = request.getParameter("file");
            String key = request.getParameter("key");
            if (fileName == null || fileName.trim().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file name");
            } else {
                downloadFile(response, fileName, key);
            }
        } else if ("sendKey".equalsIgnoreCase(action)) {
            String fileName = request.getParameter("file");
            if (fileName == null || fileName.trim().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file name");
            } else {
                sendDecryptionKey(request, response, fileName);
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        
        if ("verifyKey".equalsIgnoreCase(action)) {
            String fileName = request.getParameter("file");
            String key = request.getParameter("key");
            verifyKey(response, fileName, key);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }

    private void sendDecryptionKey(HttpServletRequest request, HttpServletResponse response, String fileName) 
            throws IOException {
        HttpSession session = request.getSession(false);
        Map<String, Object> result = new HashMap<>();
        
        if (session == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
            return;
        }
        
        // Get user email from session
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null || userEmail.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User email not found in session");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Get the encryption key from database
            String sql = "SELECT encryption_key FROM files WHERE file_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileName.replace(".enc", ""));
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String encryptionKey = rs.getString("encryption_key");
                    
                    if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
                        result.put("success", false);
                        result.put("message", "No encryption key found for this file");
                    } else {
                        // Send the key via email
                        boolean emailSent = sendKeyEmail(userEmail, fileName, encryptionKey);
                        
                        if (emailSent) {
                            result.put("success", true);
                            result.put("message", "Decryption key has been sent to  "+userEmail);
                        } else {
                            result.put("success", false);
                            result.put("message", "Failed to send decryption key");
                        }
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "File not found in database");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.setContentType("application/json");
        response.getWriter().write(new Gson().toJson(result));
    }

    private boolean sendKeyEmail(String toEmail, String fileName, String encryptionKey) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(EMAIL_SUBJECT);
            
            String emailContent = "Dear CryptoSeek User,\n\n"
                    + "You have requested the decryption key for the file: " + fileName + "\n\n"
                    + "Your decryption key is:\n " + encryptionKey + "\n\n"
                    + "Important: Keep this key secure as it is required to decrypt your file.\n"
                    + "Do not share this key with anyone.\n\n"
                    + "Best regards,\n"
                    + "CryptoSeek Team";
            
            message.setText(emailContent);
            
            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void verifyKey(HttpServletResponse response, String fileName, String key) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT encryption_key FROM files WHERE file_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileName.replace(".enc", ""));
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String storedKey = rs.getString("encryption_key");
                    
                    if (storedKey == null || storedKey.trim().isEmpty()) {
                        result.put("valid", false);
                        result.put("message", "No key found in database");
                    } else {
                        boolean isValid = storedKey.trim().equals(key.trim());
                        result.put("valid", isValid);
                        
                        if (!isValid) {
                            result.put("message", "The provided key does not match the stored key");
                        }
                    }
                } else {
                    result.put("valid", false);
                    result.put("message", "File not found in database");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("valid", false);
            result.put("message", "Database error: " + e.getMessage());
        }
        
        response.setContentType("application/json");
        response.getWriter().write(new Gson().toJson(result));
    }

    private void listFiles(HttpServletResponse response) throws IOException {
        DbxClientV2 client = DropboxConnection.getClient();
        List<String> fileNames = new ArrayList<>();

        try {
            ListFolderResult result = client.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    if (metadata instanceof FileMetadata && metadata.getName().endsWith(".enc")) {
                        fileNames.add(metadata.getName());
                    }
                }
                if (!result.getHasMore()) break;
                result = client.files().listFolderContinue(result.getCursor());
            }

            response.setContentType("application/json");
            response.getWriter().write(new Gson().toJson(fileNames));

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error listing files");
        }
    }

    private void downloadFile(HttpServletResponse response, String fileName, String key) throws IOException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT encryption_key FROM files WHERE file_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileName.replace(".enc", ""));
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String storedKey = rs.getString("encryption_key");
                    
                    // Verify the user-provided key matches the stored key
                    if (!storedKey.trim().equals(key.trim())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid decryption key");
                        return;
                    }

                    // Extract the key
                    SecretKey secretKey = extractKey(storedKey);

                    // Download and decrypt the file
                    DbxClientV2 client = DropboxConnection.getClient();

                    try (InputStream in = client.files().download("/" + fileName).getInputStream()) {
                        // Read the IV from the beginning of the file
                        byte[] iv = new byte[IV_SIZE];
                        int bytesRead = in.read(iv);
                        if (bytesRead != IV_SIZE) {
                            throw new IOException("Invalid encrypted file format - IV missing");
                        }

                        response.setContentType("application/octet-stream");
                        String downloadName = fileName.replace(".enc", "");
                        response.setHeader("Content-Disposition", 
                            "attachment; filename=\"" + downloadName + "\"");

                        decryptStream(secretKey, iv, in, response.getOutputStream());
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                        "File metadata not found in database");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Download failed: " + e.getMessage());
        }
    }

    private SecretKey extractKey(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    private void decryptStream(SecretKey key, byte[] iv, InputStream input, OutputStream output) 
            throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        try (CipherInputStream cis = new CipherInputStream(input, cipher)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    public void destroy() {
        // Clean up MySQL driver registration
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getName().equals("com.mysql.cj.jdbc.Driver")) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
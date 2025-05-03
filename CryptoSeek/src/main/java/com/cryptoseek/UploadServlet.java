package com.cryptoseek;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

@WebServlet("/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Encryption Configuration
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 16;

    // Database Configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/file_encryption";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Amol@1809";

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Part filePart = request.getPart("file");
        String originalFileName = filePart.getSubmittedFileName();
        String fileType = filePart.getContentType();

        File originalFile = File.createTempFile("upload_", "_" + originalFileName);
        File encryptedFile = File.createTempFile("encrypted_", "_" + originalFileName);

        try (InputStream input = filePart.getInputStream();
             OutputStream output = new FileOutputStream(originalFile)) {
            input.transferTo(output);
        }

        Connection dbConnection = null;
        try {
            // Generate encryption key and IV
            SecretKey secretKey = generateAESKey();
            byte[] iv = generateIV();
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            // Encrypt file with IV prepended to the file
            encryptFileWithPrependedIV(secretKey, iv, originalFile, encryptedFile);

            // Upload to Dropbox
            String encryptedFileName = originalFileName + ".enc";
            boolean uploaded = uploadToDropbox(encryptedFile, encryptedFileName);

            if (uploaded) {
                // Store metadata in database
                dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                storeFileMetadata(dbConnection, originalFileName, fileType, encodedKey);

                // Store the key in session temporarily to display in the modal
                request.getSession().setAttribute("encryptionKey", encodedKey);
                request.getSession().setAttribute("fileName", originalFileName);
                
                // Redirect to upload module with success flag
                response.sendRedirect("Dashboard.html?module=upload&upload=success");
            } else {
                response.sendRedirect("Dashboard.html?module=upload&upload=failed");
            }
        } catch (Exception e) {
            response.sendRedirect("Dashboard.html?module=upload&upload=error");
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (dbConnection != null) {
                try { dbConnection.close(); } catch (SQLException ignored) {}
            }
            originalFile.delete();
            encryptedFile.delete();
        }
    }

    private void storeFileMetadata(Connection connection, String fileName, 
                                 String fileType, String encodedKey) throws SQLException {
        String sql = "INSERT INTO files (file_name, file_type, encryption_key) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setString(2, fileType);
            stmt.setString(3, encodedKey);
            stmt.executeUpdate();
        }
    }

    private SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(KEY_SIZE, new SecureRandom());
        return generator.generateKey();
    }

    private byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private void encryptFileWithPrependedIV(SecretKey key, byte[] iv, File inputFile, File outputFile) 
            throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // First write the IV to the output file
            fos.write(iv);
            
            // Then write the encrypted content
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private boolean uploadToDropbox(File file, String remotePath) {
        try (InputStream in = new FileInputStream(file)) {
            // Use the DropboxConnection class to get the client
            DbxClientV2 client = DropboxConnection.getClient();
            
            client.files().uploadBuilder("/" + remotePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ServletException("MySQL JDBC Driver not found", e);
        }
    }

    public static SecretKey extractKey(String encodedKey) throws Exception {
        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty key string");
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey.trim());
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to decode Base64 key", e);
        }
    }
}
# 🔐 CryptoSeek

CryptoSeek is a secure, cloud-integrated file manager that enables **encrypted file upload, search, and secure download** with AES encryption. Built using **HTML, CSS, JavaScript, JSP, Servlets, and Java**, the project runs on **Tomcat Server 10.1** with **Eclipse IDE** and supports user access management through admin approval.

## 🌐 Tech Stack

- **Frontend**: HTML, CSS, JavaScript
- **Backend**: Java, JSP, Servlet
- **Server**: Apache Tomcat 10.1
- **IDE**: Eclipse
- **Database**: MySQL (`file_encryption`)
- **Cloud Integration**: Dropbox API
- **Encryption**: AES (CBC Mode with PKCS5Padding)
- **Libraries Used**: 
  - `mysql-connector-java`
  - `dropbox-core-sdk`
  - `commons-codec` (for encryption/decryption)

---

## 📁 Project Modules

### 1. 🧑‍💻 User Registration & Login
- Secure user registration with email and password
- Admin-based approval workflow
- Separate login panels for Users and Admins

### 2. 🗂️ Dashboard
- Displays real-time statistics: total uploads, downloads
- Sidebar with icon-based navigation
- Sticky search bar with semantic search

### 3. 🔐 File Upload (Encrypted)
- Upload files with semantic names and tags
- Files are encrypted using AES before uploading to Dropbox
- Encryption metadata (key + IV) is stored in the MySQL database

### 4. 🔎 File Search
- Search encrypted files using semantic tags or filenames
- List matching files with download option

### 5. ⬇️ File Download & Decryption
- Download encrypted file from Dropbox
- User provides decryption key
- Secure decryption using stored IV

### 6. ⚙️ Settings
- Update profile info
- Logout securely

---

## 🗄️ Database Structure (`file_encryption`)

### 1. `users`
- `id`, `username`, `email`, `password_hash`, `approved`, `created_at`

### 2. `admins`
- `id`, `username`, `email`, `password_hash`, `created_at`

### 3. `registration_requests`
- `id`, `username`, `email`, `password_hash`, `requested_at`

### 4. `files`
- `id`, `filename`, `semantic_name`, `tags`, `user_id`, `encryption_key_iv`, `upload_time`, `download_count`, `dropbox_path`

---

## 🚀 How to Run the Project

### Prerequisites
- Java JDK 17+
- Apache Tomcat 10.1
- MySQL Server
- Eclipse IDE
- Dropbox Developer App & Access Token

### Steps
1. Clone the repo:
   ```bash
   git clone https://github.com/yourusername/cryptoseek.git
Import project in Eclipse as a Dynamic Web Project.

Configure Tomcat Server and MySQL Database in Eclipse.

Create the database using the provided schema:

sql
Copy
Edit
CREATE DATABASE file_encryption;
-- Add table definitions and initial data
Update the DBConnection.java with your database credentials.

Add your Dropbox API token in the relevant servlet for upload/download.

Deploy the project on Tomcat Server and run on:

bash
Copy
Edit
http://localhost:8080/cryptoseek/

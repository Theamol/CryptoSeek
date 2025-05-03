let uploadInProgress = false;
let searchTimeout;

// Initialize the dashboard
document.addEventListener('DOMContentLoaded', function() {
    loadFiles();
    document.addEventListener('click', handleOutsideClick);
    
    // Check URL parameters for upload status
    const urlParams = new URLSearchParams(window.location.search);
    const uploadStatus = urlParams.get('upload');
    const module = urlParams.get('module');
    
    if (module === 'upload') {
        switchModule('upload');
    }
    
    // Check for upload status
    if (uploadStatus === 'success') {
        closeUploadProgress();
        showUploadSuccess();
        
        setTimeout(() => {
            fetch('GetKeyServlet')
                .then(response => response.json())
                .then(data => {
                    if (data.key) {
                        showKeyModal(data.fileName, data.key);
                    }
                })
                .catch(error => {
                    console.error('Error fetching encryption key:', error);
                });
        }, 1500);
    } else if (uploadStatus === 'failed') {
        closeUploadProgress();
        alert('File upload failed. Please try again.');
    } else if (uploadStatus === 'error') {
        closeUploadProgress();
        alert('An error occurred during file upload.');
    }
    
    // Setup event listeners
    setupModalEvents();
});

function setupModalEvents() {
    // Handle modal close
    document.querySelector('.close-btn').addEventListener('click', closeModalAndRefresh);
    
    // Handle click outside modal
    window.addEventListener('click', function(event) {
        if (event.target === document.getElementById('successModal')) {
            closeModalAndRefresh();
        }
    });
    
    // Enhanced copy function with auto-close
    document.getElementById('copyKeyBtn').addEventListener('click', function() {
        const keyElement = document.getElementById('successKey');
        navigator.clipboard.writeText(keyElement.textContent)
            .then(() => {
                const btn = this;
                btn.textContent = 'Copied!';
                setTimeout(closeModalAndRefresh, 800);
            })
            .catch(err => {
                console.error('Failed to copy: ', err);
                alert('Failed to copy key. Please try again.');
            });
    });
}

// AJAX upload with progress tracking
document.getElementById('uploadForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const formData = new FormData(this);
    const progressContainer = document.getElementById('progressContainer');
    const progressBar = document.getElementById('progressBar');
    const progressText = document.getElementById('progressText');
    const uploadButton = document.getElementById('uploadButton');
    
    progressContainer.style.display = 'block';
    uploadButton.disabled = true;
    
    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'upload', true);
    
    xhr.upload.onprogress = function(e) {
        if (e.lengthComputable) {
            const percentComplete = Math.round((e.loaded / e.total) * 100);
            progressBar.style.width = percentComplete + '%';
            progressText.textContent = percentComplete + '% uploaded';
        }
    };
    
    xhr.onload = function() {
        progressContainer.style.display = 'none';
        uploadButton.disabled = false;
        
        if (xhr.status === 200) {
            const response = JSON.parse(xhr.responseText);
            if (response.success) {
                document.getElementById('successFileName').textContent = response.fileName;
                document.getElementById('successKey').textContent = response.encryptionKey;
                document.getElementById('successModal').style.display = 'block';
            } else {
                alert('Upload failed: ' + response.message);
            }
        } else {
            alert('Upload error: ' + xhr.statusText);
        }
    };
    
    xhr.onerror = function() {
        progressContainer.style.display = 'none';
        uploadButton.disabled = false;
        alert('Upload failed. Please try again.');
    };
    
    xhr.send(formData);
});

//Function to close modal and refresh module
function closeModalAndRefresh() {
    // Hide modal
    document.getElementById('successModal').style.display = 'none';
    
    // Reset copy button text
    document.getElementById('copyKeyBtn').textContent = 'Copy Key';
    
    // Reset form and refresh file input
    const fileInput = document.getElementById('fileInput');
    fileInput.value = ''; // Clear the selected file
    
    // Alternative method to properly reset file input (works in all browsers)
    const form = document.getElementById('uploadForm');
    form.reset();
    
    // Create a new file input element (more reliable reset)
    const newFileInput = fileInput.cloneNode(true);
    fileInput.parentNode.replaceChild(newFileInput, fileInput);
    newFileInput.addEventListener('change', handleFileSelect); // Reattach event listener if needed
    
    // Scroll to upload module
    document.getElementById('upload').scrollIntoView({ behavior: 'smooth' });
    
    // Highlight the upload module briefly
    const uploadModule = document.getElementById('upload');
    uploadModule.style.transition = 'box-shadow 0.5s';
    uploadModule.style.boxShadow = '0 0 0 3px rgba(95, 139, 126, 0.5)';
    setTimeout(() => {
        uploadModule.style.boxShadow = 'none';
    }, 1000);
}

// Additional helper to properly reset file input
function resetFileInput(inputId) {
    const input = document.getElementById(inputId);
    if (input) {
        // Method 1: Works in modern browsers
        input.value = '';
        
        // Method 2: More reliable cross-browser solution
        const form = document.createElement('form');
        form.appendChild(input);
        form.reset();
        input.parentNode.removeChild(input);
        document.getElementById('uploadForm').appendChild(input);
    }
}

function toggleSidebar(event) {
    event.stopPropagation();
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.toggle('open');
}

function handleOutsideClick(event) {
    const sidebar = document.getElementById('sidebar');
    const hamburger = document.querySelector('.hamburger');
    const searchPopup = document.getElementById('searchPopup');
    const searchIcon = document.getElementById('searchIcon');
    
    // Close sidebar when clicking outside on mobile
    if (window.innerWidth <= 768 && 
        !sidebar.contains(event.target) && 
        !hamburger.contains(event.target)) {
        sidebar.classList.remove('open');
    }
    
    // Close search popup when clicking outside
    if (searchPopup.style.display === 'block' && 
        !searchPopup.contains(event.target) && 
        !searchIcon.contains(event.target)) {
        searchPopup.style.display = 'none';
        searchIcon.classList.remove('active');
    }
}

function switchModule(id) {
    document.querySelectorAll('.module').forEach(m => m.classList.remove('active-module'));
    document.getElementById(id).classList.add('active-module');
    
    // Close sidebar on mobile after selecting a module
    if (window.innerWidth <= 768) {
        document.getElementById('sidebar').classList.remove('open');
    }
    
    // Close search popup when switching modules
    document.getElementById('searchPopup').style.display = 'none';
    document.getElementById('searchIcon').classList.remove('active');
    
    // Load files when download or search module is opened
    if (id === 'download') {
        loadDownloadFiles();
    } else if (id === 'search') {
        loadAllFiles();
    }
}

function toggleSearch(event) {
    event.stopPropagation();
    const popup = document.getElementById('searchPopup');
    const searchIcon = document.getElementById('searchIcon');
    
    if (popup.style.display === 'block') {
        popup.style.display = 'none';
        searchIcon.classList.remove('active');
    } else {
        popup.style.display = 'block';
        searchIcon.classList.add('active');
        popup.querySelector('input').focus();
    }
}

// Load files for download module
async function loadDownloadFiles() {
    try {
        const res = await fetch('FileManagerServlet?action=list');
        const files = await res.json();
        const container = document.getElementById('downloadFileList');
        container.innerHTML = ''; // Clear existing items

        if (files.length === 0) {
            container.innerHTML = '<p>No encrypted files available</p>';
            return;
        }

        files.forEach(file => {
            const item = document.createElement('div');
            item.className = 'file-item';
            item.innerHTML = `
                <span>${file}</span>
                <button class="download-btn" onclick="promptForKey('${file}')">
                    <i class="fas fa-download"></i> Download
                </button>
            `;
            container.appendChild(item);
        });
    } catch (error) {
        console.error('Error loading files:', error);
        document.getElementById('downloadFileList').innerHTML = '<p>Error loading files. Please try again.</p>';
    }
}

// Load all files for search module
async function loadAllFiles() {
    const resultsContainer = document.getElementById('fileList');
    const loadingIndicator = document.getElementById('loadingIndicator');
    
    // Show loading indicator
    resultsContainer.innerHTML = '';
    loadingIndicator.style.display = 'block';
    
    try {
        const res = await fetch('FileManagerServlet?action=list');
        const files = await res.json();
        loadingIndicator.style.display = 'none';
        
        if (!files || files.length === 0) {
            resultsContainer.innerHTML = '<p class="no-results">No encrypted files found</p>';
        } else {
            displayFiles(files, resultsContainer);
        }
    } catch (error) {
        loadingIndicator.style.display = 'none';
        console.error('Error loading files:', error);
        resultsContainer.innerHTML = '<p class="no-results">Error loading files. Please try again.</p>';
    }
}

// Search files function with debounce
function searchFiles(query) {
    clearTimeout(searchTimeout);
    const resultsContainer = document.getElementById('fileList');
    const loadingIndicator = document.getElementById('loadingIndicator');
    
    if (!query || query.trim() === '') {
        // If search is empty, show all files
        loadAllFiles();
        return;
    }
    
    // Show loading indicator
    resultsContainer.innerHTML = '';
    loadingIndicator.style.display = 'block';
    
    searchTimeout = setTimeout(() => {
        fetch('FileManagerServlet?action=list')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(files => {
                loadingIndicator.style.display = 'none';
                
                // Filter files based on search query (case insensitive)
                const filteredFiles = files.filter(file => 
                    file.toLowerCase().includes(query.toLowerCase())
                );
                
                if (filteredFiles.length === 0) {
                    resultsContainer.innerHTML = '<p class="no-results">No files match your search</p>';
                } else {
                    displayFiles(filteredFiles, resultsContainer);
                }
            })
            .catch(error => {
                loadingIndicator.style.display = 'none';
                console.error('Error searching files:', error);
                resultsContainer.innerHTML = '<p class="no-results">Error searching files. Please try again.</p>';
            });
    }, 300); // 300ms debounce time
}

// Display files with appropriate styling (without download buttons in search)
function displayFiles(files, container) {
    // Clear existing items
    container.innerHTML = '';
    
    files.forEach(file => {
        const item = document.createElement('li');
        const fileExt = getFileExtension(file);
        const { fileTypeClass, fileIcon } = getFileTypeInfo(fileExt);
        
        item.className = `search-result-item ${fileTypeClass}`;
        
        item.innerHTML = `
            <i class="fas ${fileIcon}"></i>
            <span class="file-name">${file}</span>
        `;
        
        container.appendChild(item);
    });
}

// Quick search from the header
function quickSearchFiles(query) {
    clearTimeout(searchTimeout);
    const resultsContainer = document.getElementById('quickSearchResults');
    
    if (!query || query.trim() === '') {
        resultsContainer.innerHTML = '';
        return;
    }
    
    searchTimeout = setTimeout(() => {
        fetch('FileManagerServlet?action=list')
            .then(response => response.json())
            .then(files => {
                // Filter files based on search query
                const filteredFiles = files.filter(file => 
                    file.toLowerCase().includes(query.toLowerCase())
                );
                
                if (filteredFiles.length === 0) {
                    resultsContainer.innerHTML = '<p class="no-results">No files found</p>';
                } else {
                    // Create a simple list for quick search results
                    const list = document.createElement('ul');
                    list.style.listStyle = 'none';
                    list.style.padding = '0';
                    list.style.margin = '0';
                    
                    filteredFiles.slice(0, 5).forEach(file => {
                        const item = document.createElement('li');
                        const fileExt = getFileExtension(file);
                        const { fileTypeClass, fileIcon } = getFileTypeInfo(fileExt);
                        
                        item.style.padding = '10px';
                        item.style.cursor = 'pointer';
                        item.style.display = 'flex';
                        item.style.alignItems = 'center';
                        item.style.borderRadius = '5px';
                        item.style.margin = '5px 0';
                        item.className = fileTypeClass;
                        
                        item.innerHTML = `
                            <i class="fas ${fileIcon}" style="margin-right: 10px;"></i>
                            <span>${file}</span>
                        `;
                        
                        item.onclick = function() {
                            switchModule('search');
                            document.getElementById('fileSearchInput').value = file;
                            searchFiles(file);
                            document.getElementById('searchPopup').style.display = 'none';
                            document.getElementById('searchIcon').classList.remove('active');
                        };
                        list.appendChild(item);
                    });
                    
                    resultsContainer.innerHTML = '';
                    resultsContainer.appendChild(list);
                }
            })
            .catch(error => {
                console.error('Error in quick search:', error);
                resultsContainer.innerHTML = '<p class="no-results">Search error</p>';
            });
    }, 300);
}

// Helper function to get file extension
function getFileExtension(filename) {
    return filename.split('.').pop().toLowerCase();
}

// Helper function to determine file type styling
function getFileTypeInfo(extension) {
    const fileTypes = {
        pdf: { class: 'file-type-pdf', icon: 'fa-file-pdf' },
        doc: { class: 'file-type-doc', icon: 'fa-file-word' },
        docx: { class: 'file-type-doc', icon: 'fa-file-word' },
        xls: { class: 'file-type-xls', icon: 'fa-file-excel' },
        xlsx: { class: 'file-type-xls', icon: 'fa-file-excel' },
        jpg: { class: 'file-type-img', icon: 'fa-file-image' },
        jpeg: { class: 'file-type-img', icon: 'fa-file-image' },
        png: { class: 'file-type-img', icon: 'fa-file-image' },
        gif: { class: 'file-type-img', icon: 'fa-file-image' },
        mp3: { class: 'file-type-audio', icon: 'fa-file-audio' },
        wav: { class: 'file-type-audio', icon: 'fa-file-audio' },
        mp4: { class: 'file-type-video', icon: 'fa-file-video' },
        avi: { class: 'file-type-video', icon: 'fa-file-video' },
        zip: { class: 'file-type-zip', icon: 'fa-file-archive' },
        rar: { class: 'file-type-zip', icon: 'fa-file-archive' },
        js: { class: 'file-type-code', icon: 'fa-file-code' },
        html: { class: 'file-type-code', icon: 'fa-file-code' },
        css: { class: 'file-type-code', icon: 'fa-file-code' },
        txt: { class: 'file-type-other', icon: 'fa-file-alt' }
    };
    
    return fileTypes[extension] || { class: 'file-type-other', icon: 'fa-file' };
}

function promptForKey(fileName) {
    currentFileName = fileName;
    document.getElementById('decryptionKey').value = '';
    document.getElementById('keyModal').style.display = 'flex';
}

function closeKeyModal() {
    document.getElementById('keyModal').style.display = 'none';
}

async function verifyKey() {
    const key = document.getElementById('decryptionKey').value;
    if (!key) {
        alert('Please enter a decryption key');
        return;
    }

    try {
        const response = await fetch('FileManagerServlet?action=verifyKey', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `file=${encodeURIComponent(currentFileName)}&key=${encodeURIComponent(key)}`
        });

        if (response.ok) {
            const result = await response.json();
            if (result.valid) {
                // Key is valid, proceed with download
                window.location.href = 'FileManagerServlet?action=download&file=' + 
                    encodeURIComponent(currentFileName) + '&key=' + encodeURIComponent(key);
                closeKeyModal();
            } else {
                alert('Invalid decryption key');
            }
        } else {
            throw new Error('Key verification failed');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('An error occurred during key verification');
    }
}

function logout() {
    if (confirm("Are you sure you want to logout? All unsaved changes will be lost.")) {
        showLoading("Logging out...");
        setTimeout(() => {
            window.location.href = "Userlogin.html";
        }, 1000);
    }
}

function updateProfile() {
    const name = document.getElementById('userName').value;
    const phone = document.getElementById('userPhone').value;
    
    if (!name) {
        alert("Please enter your full name");
        return;
    }
    
    showLoading("Updating profile...");
    
    setTimeout(() => {
        hideLoading();
        alert("Profile updated successfully!");
    }, 1000);
}

function changePassword() {
    const newPassword = prompt("Enter your new password:");
    if (newPassword) {
        showLoading("Updating password...");
        setTimeout(() => {
            hideLoading();
            alert("Password changed successfully!");
        }, 1500);
    }
}

function enable2FA() {
    const enable = confirm("Do you want to enable Two-Factor Authentication?");
    if (enable) {
        showLoading("Setting up 2FA...");
        setTimeout(() => {
            hideLoading();
            alert("Two-Factor Authentication enabled successfully!\nScan the QR code with your authenticator app.");
        }, 2000);
    }
}

function showLoading(message) {
    console.log("Loading: " + message);
}

function hideLoading() {
    console.log("Loading complete");
}

// Show upload progress modal
function showUploadProgress() {
    const progressModal = document.createElement('div');
    progressModal.className = 'key-modal';
    progressModal.style.display = 'flex';
    progressModal.id = 'uploadProgressModal';
    progressModal.innerHTML = `
        <div class="key-modal-content">
            <h3>Uploading File</h3>
            <p>Please wait while your file is being encrypted and uploaded...</p>
            <div class="progress-container">
                <div class="progress-bar"></div>
            </div>
            <div class="modal-buttons">
                <button class="modal-btn cancel" onclick="cancelUpload()">
                    <i class="fas fa-times"></i> Cancel
                </button>
            </div>
        </div>
    `;

    document.body.appendChild(progressModal);
}

// Cancel upload
function cancelUpload() {
    const progressModal = document.getElementById('uploadProgressModal');
    if (progressModal) {
        progressModal.remove();
    }
    uploadInProgress = false;

    const cancelledModal = document.createElement('div');
    cancelledModal.className = 'key-modal';
    cancelledModal.style.display = 'flex';
    cancelledModal.innerHTML = `
        <div class="key-modal-content">
            <h3>Upload Cancelled</h3>
            <p>The upload process was cancelled.</p>
            <div class="modal-buttons">
                <button class="modal-btn cancel" onclick="closeKeyModal()">Close</button>
            </div>
        </div>
    `;

    document.body.appendChild(cancelledModal);
}

// Close upload progress modal
function closeUploadProgress() {
    const progressModal = document.getElementById('uploadProgressModal');
    if (progressModal) {
        progressModal.remove();
    }
}

// Show upload successful modal
function showUploadSuccess() {
    const successModal = document.createElement('div');
    successModal.className = 'key-modal';
    successModal.style.display = 'flex';
    successModal.innerHTML = `
        <div class="key-modal-content">
            <h3>Upload Successful!</h3>
            <p>Your file has been successfully encrypted and uploaded.</p>
            <div class="modal-buttons">
                <button class="modal-btn" onclick="closeSuccessModal()">OK</button>
            </div>
        </div>
    `;

    document.body.appendChild(successModal);
}

// Close success modal
function closeSuccessModal() {
    const modals = document.querySelectorAll('.key-modal');
    modals.forEach(modal => modal.remove());
}

function showKeyModal(fileName, key) {
    const keyModal = document.createElement('div');
    keyModal.className = 'key-modal';
    keyModal.style.display = 'flex';
    keyModal.innerHTML = `
        <div class="key-modal-content">
            <h3>File Uploaded Successfully</h3>
            <p>Your file <strong>${fileName}</strong> has been encrypted and stored.</p>
            <p><strong>Encryption Key:</strong> ${key}</p>
            <p style="color: red; font-weight: bold;">Please save this key as you will need it to decrypt the file!</p>
            <div class="modal-buttons">
                <button class="modal-btn" onclick="closeKeyModal()">OK</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(keyModal);
}

//Load files for download module
async function loadDownloadFiles() {
    const container = document.getElementById('downloadFileListItems');
    const loadingIndicator = document.getElementById('downloadLoadingIndicator');
    
    // Show loading indicator
    container.innerHTML = '';
    loadingIndicator.style.display = 'block';
    
    try {
        const res = await fetch('FileManagerServlet?action=list');
        const files = await res.json();
        loadingIndicator.style.display = 'none';
        
        if (!files || files.length === 0) {
            container.innerHTML = '<p class="no-results">No encrypted files found</p>';
        } else {
            displayDownloadFiles(files, container);
        }
    } catch (error) {
        loadingIndicator.style.display = 'none';
        console.error('Error loading files:', error);
        container.innerHTML = '<p class="no-results">Error loading files. Please try again.</p>';
    }
}

// Display files in download module with download and get key buttons
function displayDownloadFiles(files, container) {
    // Clear existing items
    container.innerHTML = '';
    
    files.forEach(file => {
        const item = document.createElement('li');
        const fileExt = getFileExtension(file);
        const { fileTypeClass, fileIcon } = getFileTypeInfo(fileExt);
        
        item.className = `search-result-item ${fileTypeClass}`;
        
        item.innerHTML = `
            <i class="fas ${fileIcon}"></i>
            <span class="file-name">${file}</span>
            <div class="file-actions">
                <button class="action-btn get-key-btn" onclick="getDecryptionKey('${file}')">
                    <i class="fas fa-key"></i> Get Key
                </button>
                <button class="action-btn download-btn" onclick="promptForKey('${file}')">
                    <i class="fas fa-download"></i> Download
                </button>
            </div>
        `;
        
        container.appendChild(item);
    });
}

// Get decryption key via email
function getDecryptionKey(fileName) {
    showLoading("Sending decryption key to your email...");
    
    fetch('FileManagerServlet?action=sendKey&file=' + encodeURIComponent(fileName))
        .then(response => response.json())
        .then(data => {
            hideLoading();
            if (data.success) {
                alert(data.message);
            } else {
                alert("Error: " + data.message);
            }
        })
        .catch(error => {
            hideLoading();
            console.error('Error:', error);
            alert("Failed to send decryption key. Please try again.");
        });
}

//Display files in download module with responsive layout
function displayDownloadFiles(files, container) {
    // Clear existing items
    container.innerHTML = '';
    
    files.forEach(file => {
        const item = document.createElement('li');
        const fileExt = getFileExtension(file);
        const { fileTypeClass, fileIcon } = getFileTypeInfo(fileExt);
        
        item.className = `file-item ${fileTypeClass}`;
        
        item.innerHTML = `
            <div class="file-name-container">
                <i class="fas ${fileIcon}"></i>
                <span class="file-name">${file}</span>
            </div>
            <div class="file-actions">
                <button class="action-btn get-key-btn" onclick="getDecryptionKey('${file}')">
                    <i class="fas fa-key"></i> <span class="btn-text">Get Key</span>
                </button>
                <button class="action-btn download-btn" onclick="promptForKey('${file}')">
                    <i class="fas fa-download"></i> <span class="btn-text">Download</span>
                </button>
            </div>
        `;
        
        container.appendChild(item);
    });
}

//Update Profile Function
async function updateProfile() {
 const newName = document.getElementById('userName').value.trim();
 
 if (!newName) {
     alert("Please enter your full name");
     return;
 }
 
 showLoading("Updating profile...");
 
 try {
     const response = await fetch('SettingsServlet', {
         method: 'POST',
         headers: {
             'Content-Type': 'application/x-www-form-urlencoded',
         },
         body: `action=updateProfile&name=${encodeURIComponent(newName)}`
     });
     
     const result = await response.json();
     
     if (result.success) {
         alert("Profile updated successfully!");
     } else {
         alert("Error: " + result.message);
     }
 } catch (error) {
     console.error('Error updating profile:', error);
     alert("Failed to update profile. Please try again.");
 } finally {
     hideLoading();
 }
}

//Change Password Function
async function changePassword() {
 const currentPass = document.getElementById('currentPassword').value;
 const newPass = document.getElementById('newPassword').value;
 const confirmPass = document.getElementById('confirmPassword').value;
 const errorElement = document.getElementById('passwordError');
 
 // Clear previous errors
 errorElement.textContent = '';
 
 // Validate inputs
 if (!currentPass || !newPass || !confirmPass) {
     errorElement.textContent = 'Please fill in all password fields';
     return;
 }
 
 if (newPass !== confirmPass) {
     errorElement.textContent = 'New passwords do not match';
     return;
 }
 
 if (newPass.length < 8) {
     errorElement.textContent = 'Password must be at least 8 characters';
     return;
 }
 
 showLoading("Updating password...");
 
 try {
     const response = await fetch('SettingsServlet', {
         method: 'POST',
         headers: {
             'Content-Type': 'application/x-www-form-urlencoded',
         },
         body: `action=changePassword&currentPassword=${encodeURIComponent(currentPass)}&newPassword=${encodeURIComponent(newPass)}`
     });
     
     const result = await response.json();
     
     if (result.success) {
         // Clear password fields on success
         document.getElementById('currentPassword').value = '';
         document.getElementById('newPassword').value = '';
         document.getElementById('confirmPassword').value = '';
         
         alert("Password changed successfully!");
     } else {
         errorElement.textContent = result.message || "Failed to change password";
     }
 } catch (error) {
     console.error('Error changing password:', error);
     errorElement.textContent = "An error occurred. Please try again.";
 } finally {
     hideLoading();
 }
}

//Session timeout handling
let timeoutWarning = setTimeout(function() {
    alert("Your session will expire in 2 minutes. Please save your work.");
}, 13 * 60 * 1000); // 13 minutes

let timeoutRedirect = setTimeout(function() {
    window.location.href = "SessionExpired.html";
}, 15 * 60 * 1000); // 15 minutes

// Reset timers on user activity
document.addEventListener('mousemove', resetTimers);
document.addEventListener('keypress', resetTimers);

function resetTimers() {
    clearTimeout(timeoutWarning);
    clearTimeout(timeoutRedirect);

    timeoutWarning = setTimeout(function() {
        alert("Your session will expire in 2 minutes. Please save your work.");
    }, 13 * 60 * 1000);

    timeoutRedirect = setTimeout(function() {
        window.location.href = "SessionExpired.html";
    }, 15 * 60 * 1000);
}
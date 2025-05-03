
        document.addEventListener('DOMContentLoaded', function() {
            // Mobile Menu Toggle
            const hamburger = document.querySelector('.hamburger');
            const navLinks = document.querySelector('.nav-links');
            
            hamburger.addEventListener('click', function() {
                this.classList.toggle('active');
                navLinks.classList.toggle('active');
            });
            
            // Modal Functionality
            const userModal = document.getElementById('userAuthModal');
            const adminModal = document.getElementById('adminAuthModal');
            const openUserModal = document.getElementById('openUserModal');
            const openAdminModal = document.getElementById('openAdminModal');
            const closeModals = document.querySelectorAll('.close-modal');
            
            openUserModal.addEventListener('click', function() {
                userModal.classList.add('active');
                document.body.style.overflow = 'hidden';
            });
            
            openAdminModal.addEventListener('click', function() {
                adminModal.classList.add('active');
                document.body.style.overflow = 'hidden';
            });
            
            closeModals.forEach(closeBtn => {
                closeBtn.addEventListener('click', function() {
                    this.closest('.modal').classList.remove('active');
                    document.body.style.overflow = '';
                });
            });
            
            // Close modal when clicking outside
            window.addEventListener('click', function(e) {
                if (e.target.classList.contains('modal')) {
                    e.target.classList.remove('active');
                    document.body.style.overflow = '';
                }
            });
            
            // Flip effect for auth forms
            const switchButtons = document.querySelectorAll('.switch-btn');
            
            switchButtons.forEach(button => {
                button.addEventListener('click', function(e) {
                    e.preventDefault();
                    const target = this.getAttribute('data-target');
                    const container = this.closest('.auth-container');
                    
                    container.classList.toggle('flipped');
                    
                    // Reset forms when flipping
                    setTimeout(() => {
                        const form = container.querySelector(`#${target} form`);
                        if (form) form.reset();
                    }, 300);
                });
            });
            
            // Form submissions
            const forms = document.querySelectorAll('form');
            
            forms.forEach(form => {
                form.addEventListener('submit', function(e) {
                    e.preventDefault();
                    
                    // In a real application, you would handle form submission here
                    // For demo purposes, we'll just show an alert
                    const formType = this.id;
                    let message = '';
                    
                    switch(formType) {
                        case 'userLoginForm':
                            message = 'User login request received. In a real app, this would authenticate the user.';
                            break;
                        case 'userRegisterForm':
                            message = 'User registration request received. In a real app, this would create a new account.';
                            break;
                        case 'adminLoginForm':
                            message = 'Admin login request received. This would verify admin credentials in a real app.';
                            break;
                        case 'adminHelpForm':
                            message = 'Admin assistance request received. Security team would be notified in a real app.';
                            break;
                        case 'contactForm':
                            message = 'Thank you for your message! Our security team will respond to your inquiry shortly.';
                            break;
                        default:
                            message = 'Form submitted successfully!';
                    }
                    
                    alert(message);
                    
                    // Close modals after form submission
                    if (form.closest('.modal-content')) {
                        form.closest('.modal').classList.remove('active');
                        document.body.style.overflow = '';
                    }
                    
                    // Reset the form
                    this.reset();
                });
            });
            
            // Smooth scrolling for anchor links
            document.querySelectorAll('a[href^="#"]').forEach(anchor => {
                anchor.addEventListener('click', function(e) {
                    e.preventDefault();
                    
                    const targetId = this.getAttribute('href');
                    if (targetId === '#') return;
                    
                    const targetElement = document.querySelector(targetId);
                    if (targetElement) {
                        // Close mobile menu if open
                        hamburger.classList.remove('active');
                        navLinks.classList.remove('active');
                        
                        window.scrollTo({
                            top: targetElement.offsetTop - 80,
                            behavior: 'smooth'
                        });
                    }
                });
            });
            
            // Add active class to nav links based on scroll position
            const sections = document.querySelectorAll('section');
            const navItems = document.querySelectorAll('.nav-links a');
            
            window.addEventListener('scroll', function() {
                let current = '';
                
                sections.forEach(section => {
                    const sectionTop = section.offsetTop;
                    const sectionHeight = section.clientHeight;
                    
                    if (pageYOffset >= (sectionTop - 100)) {
                        current = section.getAttribute('id');
                    }
                });
                
                navItems.forEach(item => {
                    item.classList.remove('active');
                    if (item.getAttribute('href') === `#${current}`) {
                        item.classList.add('active');
                    }
                });
            });
            
            // Hero image hover effect
            const heroImage = document.querySelector('.hero-image img');
            if (heroImage) {
                heroImage.addEventListener('mouseenter', function() {
                    this.style.transform = 'perspective(1000px) rotateY(0deg)';
                });
                
                heroImage.addEventListener('mouseleave', function() {
                    this.style.transform = 'perspective(1000px) rotateY(-15deg)';
                });
            }
            
            // Animate feature cards on scroll
            const featureCards = document.querySelectorAll('.feature-card');
            
            function animateOnScroll() {
                featureCards.forEach(card => {
                    const cardPosition = card.getBoundingClientRect().top;
                    const screenPosition = window.innerHeight / 1.3;
                    
                    if (cardPosition < screenPosition) {
                        card.style.opacity = '1';
                        card.style.transform = 'translateY(0)';
                    }
                });
            }
            
            // Set initial state for animation
            featureCards.forEach(card => {
                card.style.opacity = '0';
                card.style.transform = 'translateY(20px)';
                card.style.transition = 'all 0.5s ease';
            });
            
            window.addEventListener('scroll', animateOnScroll);
            animateOnScroll(); // Run once on page load
        });
    
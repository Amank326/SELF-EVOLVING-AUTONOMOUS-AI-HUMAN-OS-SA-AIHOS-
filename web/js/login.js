/**
 * Login Form Handling
 * Manages login form validation, submission, and animations
 */

class LoginManager {
    constructor(animationController) {
        this.animationController = animationController;
        this.formData = {
            username: '',
            password: '',
            rememberMe: false
        };

        this.errors = {};
        this.isSubmitting = false;

        this.init();
    }

    /**
     * Initialize login form
     */
    init() {
        this.attachEventListeners();
        this.createParticles();
        this.loadRememberedUsername();
    }

    /**
     * Attach event listeners to form elements
     */
    attachEventListeners() {
        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        const rememberCheckbox = document.getElementById('rememberMe');
        const loginButton = document.querySelector('.login-button');
        const loginForm = document.getElementById('loginForm');

        // Username input
        if (usernameInput) {
            usernameInput.addEventListener('focus', () => {
                this.animationController.animateInputFocus(usernameInput);
            });

            usernameInput.addEventListener('blur', () => {
                this.animationController.animateInputBlur(usernameInput);
            });

            usernameInput.addEventListener('input', (e) => {
                this.formData.username = e.target.value;
                this.validateUsername();
            });
        }

        // Password input
        if (passwordInput) {
            passwordInput.addEventListener('focus', () => {
                this.animationController.animateInputFocus(passwordInput);
            });

            passwordInput.addEventListener('blur', () => {
                this.animationController.animateInputBlur(passwordInput);
            });

            passwordInput.addEventListener('input', (e) => {
                this.formData.password = e.target.value;
                this.validatePassword();
            });

            // Enter key to login
            passwordInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.submitLogin();
                }
            });
        }

        // Remember me checkbox
        if (rememberCheckbox) {
            rememberCheckbox.addEventListener('change', (e) => {
                this.formData.rememberMe = e.target.checked;
            });
        }

        // Login form submit
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.submitLogin();
            });
        }

        // Login button
        if (loginButton) {
            loginButton.addEventListener('click', (e) => {
                e.preventDefault();
                this.submitLogin();
            });
        }
    }

    /**
     * Create particle system for login screen
     */
    createParticles() {
        const particlesContainer = document.getElementById('particles');
        if (!particlesContainer) return;

        const particleCount = 50;

        for (let i = 0; i < particleCount; i++) {
            const particle = document.createElement('div');
            particle.className = 'particle';

            const x = Math.random() * window.innerWidth;
            const y = Math.random() * window.innerHeight;

            particle.style.left = x + 'px';
            particle.style.top = y + 'px';

            particlesContainer.appendChild(particle);

            // Animate particles
            gsap.to(particle, {
                duration: gsap.utils.random(3, 8),
                x: gsap.utils.random(-300, 300),
                y: gsap.utils.random(-300, 300),
                opacity: 0,
                repeat: -1,
                yoyo: true,
                ease: 'sine.inOut'
            });
        }
    }

    /**
     * Validate username format
     */
    validateUsername() {
        const username = this.formData.username;
        const usernameInput = document.getElementById('username');

        if (!username) {
            this.errors.username = 'Username is required';
            this.showFieldError(usernameInput, 'Username is required');
            return false;
        }

        if (username.length < 3) {
            this.errors.username = 'Username must be at least 3 characters';
            this.showFieldError(usernameInput, 'Minimum 3 characters');
            return false;
        }

        delete this.errors.username;
        this.clearFieldError(usernameInput);
        return true;
    }

    /**
     * Validate password
     */
    validatePassword() {
        const password = this.formData.password;
        const passwordInput = document.getElementById('password');

        if (!password) {
            this.errors.password = 'Password is required';
            this.showFieldError(passwordInput, 'Password is required');
            return false;
        }

        if (password.length < 6) {
            this.errors.password = 'Password must be at least 6 characters';
            this.showFieldError(passwordInput, 'Minimum 6 characters');
            return false;
        }

        delete this.errors.password;
        this.clearFieldError(passwordInput);
        return true;
    }

    /**
     * Show field error
     */
    showFieldError(input, message) {
        input.style.borderBottomColor = 'var(--danger)';
        
        let errorElement = input.parentElement.querySelector('.field-error');
        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.className = 'field-error';
            errorElement.style.cssText = `
                color: var(--danger);
                font-size: 12px;
                margin-top: 4px;
                opacity: 0;
                animation: fadeIn 0.3s ease-out forwards;
            `;
            input.parentElement.appendChild(errorElement);
        }

        errorElement.textContent = message;
    }

    /**
     * Clear field error
     */
    clearFieldError(input) {
        input.style.borderBottomColor = '';
        
        const errorElement = input.parentElement.querySelector('.field-error');
        if (errorElement) {
            gsap.to(errorElement, {
                duration: 0.2,
                opacity: 0,
                onComplete: () => errorElement.remove()
            });
        }
    }

    /**
     * Submit login form
     */
    async submitLogin() {
        if (this.isSubmitting) return;

        // Validate form
        const usernameValid = this.validateUsername();
        const passwordValid = this.validatePassword();

        if (!usernameValid || !passwordValid) {
            this.shakeForm();
            return;
        }

        this.isSubmitting = true;

        // Animate button
        this.animationController.animateButtonClick();

        try {
            // Simulate API call
            await new Promise(resolve => setTimeout(resolve, 1500));

            // Save credentials if remember me is checked
            if (this.formData.rememberMe) {
                Utils.storage.set('rememberedUsername', this.formData.username);
            } else {
                Utils.storage.remove('rememberedUsername');
            }

            // Emit login success
            this.onLoginSuccess();

        } catch (error) {
            this.showLoginError('Login failed. Please try again.');
        } finally {
            this.isSubmitting = false;
        }
    }

    /**
     * Show login error message
     */
    showLoginError(message) {
        const loginForm = document.querySelector('.login-form-wrapper');
        
        let errorAlert = document.querySelector('.login-error');
        if (!errorAlert) {
            errorAlert = document.createElement('div');
            errorAlert.className = 'login-error';
            errorAlert.style.cssText = `
                background: rgba(255, 0, 85, 0.1);
                border: 1px solid var(--danger);
                border-radius: 8px;
                padding: 12px;
                margin-bottom: 20px;
                color: var(--danger);
                font-size: 12px;
                opacity: 0;
                animation: slideInCard 0.4s ease-out forwards;
            `;
            loginForm.insertBefore(errorAlert, loginForm.firstChild);
        }

        errorAlert.textContent = message;

        gsap.timeline()
            .to(errorAlert, { opacity: 1, duration: 0.3 })
            .to(errorAlert, { opacity: 0, duration: 0.3 }, '+=2.5');
    }

    /**
     * Shake form on error
     */
    shakeForm() {
        const form = document.querySelector('.login-form-wrapper');

        gsap.timeline()
            .to(form, { x: -10, duration: 0.1 })
            .to(form, { x: 10, duration: 0.1 })
            .to(form, { x: -10, duration: 0.1 })
            .to(form, { x: 0, duration: 0.1 });
    }

    /**
     * Handle successful login
     */
    onLoginSuccess() {
        Utils.log.success('Login successful!');
        
        const loginScreen = document.getElementById('loginScreen');
        const dashboardScreen = document.getElementById('dashboardScreen');

        // Animate transition
        const loginForm = document.querySelector('.login-form-wrapper');
        
        gsap.timeline()
            .to(loginForm, {
                duration: 0.5,
                scale: 0.95,
                opacity: 0,
                ease: 'power2.in'
            })
            .call(() => {
                loginScreen.classList.remove('active');
                dashboardScreen.classList.add('active');
                
                // Emit custom event
                window.dispatchEvent(new CustomEvent('loginSuccess', {
                    detail: { username: this.formData.username }
                }));
            }, null, 0.3);
    }

    /**
     * Load remembered username
     */
    loadRememberedUsername() {
        const rememberedUsername = Utils.storage.get('rememberedUsername');
        const usernameInput = document.getElementById('username');
        const rememberCheckbox = document.getElementById('rememberMe');

        if (rememberedUsername && usernameInput) {
            usernameInput.value = rememberedUsername;
            this.formData.username = rememberedUsername;
            
            if (rememberCheckbox) {
                rememberCheckbox.checked = true;
                this.formData.rememberMe = true;
            }
        }
    }

    /**
     * Login with Google via Firebase
     */
    async loginWithGoogle() {
        const googleBtn = document.getElementById('googleLoginBtn');
        if (!googleBtn || googleBtn.classList.contains('loading')) return;

        googleBtn.classList.add('loading');

        try {
            if (typeof firebase !== 'undefined' && firebase.auth) {
                const provider = new firebase.auth.GoogleAuthProvider();
                provider.addScope('profile');
                provider.addScope('email');

                const result = await firebase.auth().signInWithPopup(provider);
                const user = result.user;

                Utils.log.success(`Google login successful: ${user.displayName}`);
                this.onSocialLoginSuccess({
                    provider: 'google',
                    uid: user.uid,
                    displayName: user.displayName,
                    email: user.email,
                    photoURL: user.photoURL,
                    token: await user.getIdToken()
                });
            } else {
                // Fallback: Redirect to Google OAuth
                this.showLoginError('Firebase not loaded. Please check your connection and try again.');
            }
        } catch (error) {
            console.error('Google login error:', error);
            if (error.code === 'auth/popup-closed-by-user') {
                // User closed the popup, no error needed
                return;
            } else if (error.code === 'auth/unauthorized-domain') {
                this.showLoginError('This domain is not authorized for Google login. Add it to Firebase Console → Auth → Settings → Authorized domains.');
            } else {
                this.showLoginError(`Google login failed: ${error.message || 'Unknown error'}`);
            }
        } finally {
            googleBtn.classList.remove('loading');
        }
    }

    /**
     * Login with GitHub via Firebase
     */
    async loginWithGitHub() {
        const githubBtn = document.getElementById('githubLoginBtn');
        if (!githubBtn || githubBtn.classList.contains('loading')) return;

        githubBtn.classList.add('loading');

        try {
            if (typeof firebase !== 'undefined' && firebase.auth) {
                const provider = new firebase.auth.GithubAuthProvider();
                provider.addScope('read:user');
                provider.addScope('user:email');

                const result = await firebase.auth().signInWithPopup(provider);
                const user = result.user;

                Utils.log.success(`GitHub login successful: ${user.displayName}`);
                this.onSocialLoginSuccess({
                    provider: 'github',
                    uid: user.uid,
                    displayName: user.displayName || user.email,
                    email: user.email,
                    photoURL: user.photoURL,
                    token: await user.getIdToken()
                });
            } else {
                this.showLoginError('Firebase not loaded. Please check your connection and try again.');
            }
        } catch (error) {
            console.error('GitHub login error:', error);
            if (error.code === 'auth/popup-closed-by-user') {
                return;
            } else if (error.code === 'auth/account-exists-with-different-credential') {
                this.showLoginError('An account already exists with the same email. Try logging in with a different provider.');
            } else if (error.code === 'auth/unauthorized-domain') {
                this.showLoginError('This domain is not authorized for GitHub login. Add it to Firebase Console → Auth → Settings → Authorized domains.');
            } else {
                this.showLoginError(`GitHub login failed: ${error.message || 'Unknown error'}`);
            }
        } finally {
            githubBtn.classList.remove('loading');
        }
    }

    /**
     * Handle successful social login (Google/GitHub)
     */
    onSocialLoginSuccess(userData) {
        // Store user data in session
        Utils.storage.set('authUser', JSON.stringify({
            provider: userData.provider,
            uid: userData.uid,
            displayName: userData.displayName,
            email: userData.email,
            photoURL: userData.photoURL,
            loginTime: Date.now()
        }));
        Utils.storage.set('authToken', userData.token);

        // Set form username for transition
        this.formData.username = userData.displayName || userData.email;

        // Navigate to dashboard
        this.onLoginSuccess();
    }

    /**
     * Reset login form
     */
    reset() {
        this.formData = {
            username: '',
            password: '',
            rememberMe: false
        };

        this.errors = {};

        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        const rememberCheckbox = document.getElementById('rememberMe');

        if (usernameInput) usernameInput.value = '';
        if (passwordInput) passwordInput.value = '';
        if (rememberCheckbox) rememberCheckbox.checked = false;

        this.clearFieldError(usernameInput);
        this.clearFieldError(passwordInput);
    }
}

// Export
window.LoginManager = LoginManager;

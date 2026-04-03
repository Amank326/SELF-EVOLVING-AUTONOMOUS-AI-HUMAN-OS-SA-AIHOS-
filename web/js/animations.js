/**
 * Animation Controller Class
 * Manages all GSAP animations for the application
 */

class AnimationController {
    constructor() {
        this.timeline = null;
        this.loadingTimeline = null;
        this.loginTimeline = null;
        this.dashboardTimeline = null;
    }

    /**
     * Initialize all animations
     */
    initAnimations() {
        this.createLoadingAnimation();
        this.createLoginAnimation();
    }

    /**
     * Create loading screen animations
     */
    createLoadingAnimation() {
        this.loadingTimeline = gsap.timeline();

        // Stagger spinner rings
        this.loadingTimeline.to('.spinner-ring', {
            duration: 0.3,
            opacity: 1,
            stagger: 0.1
        }, 0);

        // Animate loading text
        this.loadingTimeline.to('.loading-text', {
            duration: 0.6,
            opacity: 1,
            y: 0
        }, 0.2);

        // Progress animation
        this.loadingTimeline.to('.loading-progress', {
            duration: 3,
            width: '100%',
            ease: 'power1.inOut'
        }, 0.2);
    }

    /**
     * Create login screen animations
     */
    createLoginAnimation() {
        this.loginTimeline = gsap.timeline();

        // Gradient spheres floating animation
        this.loginTimeline.to('#sphere1', {
            duration: 8,
            x: 50,
            y: 50,
            repeat: -1,
            yoyo: true,
            ease: 'sine.inOut'
        }, 0);

        this.loginTimeline.to('#sphere2', {
            duration: 10,
            x: -50,
            y: -50,
            repeat: -1,
            yoyo: true,
            ease: 'sine.inOut'
        }, 0);

        this.loginTimeline.to('#sphere3', {
            duration: 12,
            x: 30,
            y: -30,
            repeat: -1,
            yoyo: true,
            ease: 'sine.inOut'
        }, 0);

        // Form entrance
        this.loginTimeline.from('.login-form-wrapper', {
            duration: 0.8,
            opacity: 0,
            y: 30,
            ease: 'back.out'
        }, 0.3);

        // Form elements stagger
        this.loginTimeline.from('.form-group', {
            duration: 0.4,
            opacity: 0,
            x: -20,
            stagger: 0.1,
            ease: 'power2.out'
        }, 0.5);

        // Button entrance
        this.loginTimeline.from('.login-button', {
            duration: 0.6,
            opacity: 0,
            scale: 0.9,
            ease: 'back.out'
        }, '-=0.3');
    }

    /**
     * Animate form input focus
     */
    animateInputFocus(input) {
        gsap.to(input, {
            duration: 0.3,
            scale: 1.02,
            ease: 'power2.out'
        });
    }

    /**
     * Animate form input blur
     */
    animateInputBlur(input) {
        gsap.to(input, {
            duration: 0.3,
            scale: 1,
            ease: 'power2.out'
        });
    }

    /**
     * Animate login button click
     */
    animateButtonClick() {
        const timeline = gsap.timeline();

        timeline.to('.login-button', {
            duration: 0.1,
            scale: 0.95,
            ease: 'power2.in'
        });

        timeline.to('.login-button', {
            duration: 0.2,
            scale: 1,
            ease: 'power2.out'
        });

        return timeline;
    }

    /**
     * Animate screen transition
     */
    animateScreenTransition(fromScreen, toScreen) {
        const timeline = gsap.timeline();

        timeline.to(fromScreen, {
            duration: 0.4,
            opacity: 0,
            ease: 'power1.inOut'
        }, 0);

        timeline.call(() => {
            fromScreen.classList.remove('active');
            toScreen.classList.add('active');
        }, null, 0.2);

        timeline.from(toScreen, {
            duration: 0.6,
            opacity: 0,
            ease: 'power1.inOut'
        }, 0.2);

        return timeline;
    }

    /**
     * Animate card hover
     */
    animateCardHover(card) {
        gsap.to(card, {
            duration: 0.4,
            y: -8,
            boxShadow: '0 12px 40px rgba(0, 217, 255, 0.15)',
            ease: 'power2.out'
        });
    }

    /**
     * Animate card hover out
     */
    animateCardHoverOut(card) {
        gsap.to(card, {
            duration: 0.4,
            y: 0,
            boxShadow: '0 0 0 rgba(0, 217, 255, 0)',
            ease: 'power2.out'
        });
    }
}

// Export for use
window.AnimationController = AnimationController;

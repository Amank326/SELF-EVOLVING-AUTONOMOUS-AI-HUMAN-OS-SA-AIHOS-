/**
 * Three.js 3D Scene Setup
 * Handles all 3D rendering, animations, and interactive elements
 */

class ThreeJSScene {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.scene = null;
        this.camera = null;
        this.renderer = null;
        this.particles = null;
        this.objects = [];
        this.mouse = { x: 0, y: 0 };
        this.targetMouse = { x: 0, y: 0 };
        
        this.init();
    }

    /**
     * Initialize Three.js scene
     */
    init() {
        // Scene setup
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x0a0a0a);
        this.scene.fog = new THREE.Fog(0x0a0a0a, 2000, 10000);

        // Camera setup
        this.setupCamera();

        // Renderer setup
        this.setupRenderer();

        // Add lights
        this.addLights();

        // Add objects
        this.addObjects();

        // Add particles
        this.createParticles();

        // Handle window resize
        window.addEventListener('resize', () => this.onWindowResize());

        // Track mouse movement
        document.addEventListener('mousemove', (e) => this.onMouseMove(e));

        // Start animation loop
        this.animate();
    }

    /**
     * Setup camera
     */
    setupCamera() {
        const width = this.container.clientWidth;
        const height = this.container.clientHeight;

        this.camera = new THREE.PerspectiveCamera(
            75,
            width / height,
            0.1,
            10000
        );

        this.camera.position.set(0, 0, 100);
        this.camera.lookAt(0, 0, 0);
    }

    /**
     * Setup WebGL renderer
     */
    setupRenderer() {
        const width = this.container.clientWidth;
        const height = this.container.clientHeight;

        this.renderer = new THREE.WebGLRenderer({
            antialias: true,
            alpha: true,
            precision: 'highp'
        });

        this.renderer.setSize(width, height);
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.outputEncoding = THREE.sRGBEncoding;

        this.container.appendChild(this.renderer.domElement);
    }

    /**
     * Add lighting to scene
     */
    addLights() {
        // Ambient light
        const ambientLight = new THREE.AmbientLight(0xffffff, 0.4);
        this.scene.add(ambientLight);

        // Directional light
        const directionalLight = new THREE.DirectionalLight(0x00d9ff, 0.8);
        directionalLight.position.set(100, 100, 100);
        this.scene.add(directionalLight);

        // Point light (primary color)
        const pointLight1 = new THREE.PointLight(0x00d9ff, 1, 500);
        pointLight1.position.set(100, 0, 0);
        this.scene.add(pointLight1);

        // Point light (secondary color)
        const pointLight2 = new THREE.PointLight(0xff006e, 0.8, 500);
        pointLight2.position.set(-100, 100, 0);
        this.scene.add(pointLight2);

        // Point light (accent color)
        const pointLight3 = new THREE.PointLight(0x8f00ff, 0.8, 500);
        pointLight3.position.set(0, -100, 100);
        this.scene.add(pointLight3);
    }

    /**
     * Add 3D objects to scene
     */
    addObjects() {
        // Create gradient spheres
        this.createGradientSpheres();

        // Create neural network visualization
        this.createNeuralNetwork();

        // Create floating cubes
        this.createFloatingCubes();

        // Create rings
        this.createRings();
    }

    /**
     * Create gradient spheres
     */
    createGradientSpheres() {
        const sphereGeometry = new THREE.IcosahedronGeometry(40, 16);

        // Sphere 1 - Cyan
        const material1 = new THREE.MeshPhongMaterial({
            color: 0x00d9ff,
            emissive: 0x00d9ff,
            emissiveIntensity: 0.3,
            wireframe: false,
            transparent: true,
            opacity: 0.6
        });
        const sphere1 = new THREE.Mesh(sphereGeometry, material1);
        sphere1.position.set(-150, 100, -200);
        sphere1.userData.target = {
            x: gsap.utils.random(-200, -100),
            y: gsap.utils.random(50, 150),
            z: gsap.utils.random(-250, -150)
        };
        this.scene.add(sphere1);
        this.objects.push(sphere1);

        // Sphere 2 - Pink
        const material2 = new THREE.MeshPhongMaterial({
            color: 0xff006e,
            emissive: 0xff006e,
            emissiveIntensity: 0.2,
            wireframe: false,
            transparent: true,
            opacity: 0.4
        });
        const sphere2 = new THREE.Mesh(sphereGeometry, material2);
        sphere2.position.set(150, -100, -200);
        sphere2.scale.set(0.8, 0.8, 0.8);
        sphere2.userData.target = {
            x: gsap.utils.random(100, 200),
            y: gsap.utils.random(-150, -50),
            z: gsap.utils.random(-250, -150)
        };
        this.scene.add(sphere2);
        this.objects.push(sphere2);

        // Sphere 3 - Purple
        const material3 = new THREE.MeshPhongMaterial({
            color: 0x8f00ff,
            emissive: 0x8f00ff,
            emissiveIntensity: 0.25,
            wireframe: false,
            transparent: true,
            opacity: 0.5
        });
        const sphere3 = new THREE.Mesh(sphereGeometry, material3);
        sphere3.position.set(0, 0, -150);
        sphere3.scale.set(0.6, 0.6, 0.6);
        sphere3.userData.target = {
            x: gsap.utils.random(-50, 50),
            y: gsap.utils.random(-50, 50),
            z: gsap.utils.random(-200, -100)
        };
        this.scene.add(sphere3);
        this.objects.push(sphere3);

        // Animate spheres
        this.objects.slice(-3).forEach((sphere, index) => {
            gsap.to(sphere.position, {
                x: sphere.userData.target.x,
                y: sphere.userData.target.y,
                z: sphere.userData.target.z,
                duration: gsap.utils.random(8, 12),
                repeat: -1,
                yoyo: true,
                ease: 'sine.inOut'
            });

            gsap.to(sphere.rotation, {
                x: Math.PI * 2,
                y: Math.PI * 2,
                z: Math.PI * 2,
                duration: gsap.utils.random(15, 25),
                repeat: -1,
                ease: 'none'
            });
        });
    }

    /**
     * Create neural network visualization
     */
    createNeuralNetwork() {
        const nodeCount = 30;
        const nodes = [];
        const edges = [];

        // Create nodes
        const nodeGeometry = new THREE.SphereGeometry(2, 8, 8);
        const nodeMaterial = new THREE.MeshBasicMaterial({ color: 0x00d9ff });

        for (let i = 0; i < nodeCount; i++) {
            const x = gsap.utils.random(-150, 150);
            const y = gsap.utils.random(-150, 150);
            const z = gsap.utils.random(-300, -50);

            const node = new THREE.Mesh(nodeGeometry, nodeMaterial.clone());
            node.position.set(x, y, z);
            node.userData.originalPos = { x, y, z };
            node.userData.targetPos = { x, y, z };

            this.scene.add(node);
            nodes.push(node);
        }

        // Create edges between nearby nodes
        const lineGeometry = new THREE.BufferGeometry();
        const lineMaterial = new THREE.LineBasicMaterial({
            color: 0x00d9ff,
            transparent: true,
            opacity: 0.3,
            linewidth: 1
        });

        for (let i = 0; i < nodes.length; i++) {
            const connections = gsap.utils.random(1, 3);
            for (let j = 0; j < connections; j++) {
                const targetNode = nodes[Math.floor(Math.random() * nodes.length)];
                const line = new THREE.Line(
                    new THREE.BufferGeometry().setFromPoints([
                        nodes[i].position,
                        targetNode.position
                    ]),
                    lineMaterial.clone()
                );
                this.scene.add(line);
                edges.push(line);
            }
        }

        // Animate nodes
        nodes.forEach((node, index) => {
            gsap.to(node.position, {
                x: () => gsap.utils.random(-180, 180),
                y: () => gsap.utils.random(-180, 180),
                z: () => gsap.utils.random(-300, -50),
                duration: gsap.utils.random(6, 12),
                repeat: -1,
                yoyo: true,
                ease: 'sine.inOut',
                delay: index * 0.05
            });

            gsap.to(node.material, {
                emissive: new THREE.Color(0x00d9ff),
                duration: gsap.utils.random(2, 4),
                repeat: -1,
                yoyo: true,
                ease: 'sine.inOut'
            });
        });

        this.objects.push({ nodes, edges });
    }

    /**
     * Create floating cubes
     */
    createFloatingCubes() {
        const cubeCount = 12;
        const cubeGeometry = new THREE.BoxGeometry(20, 20, 20);

        const colors = [0x00d9ff, 0xff006e, 0x8f00ff];

        for (let i = 0; i < cubeCount; i++) {
            const material = new THREE.MeshPhongMaterial({
                color: colors[i % colors.length],
                emissive: colors[i % colors.length],
                emissiveIntensity: 0.2,
                wireframe: false,
                transparent: true,
                opacity: 0.7
            });

            const cube = new THREE.Mesh(cubeGeometry, material);
            const x = gsap.utils.random(-200, 200);
            const y = gsap.utils.random(-200, 200);
            const z = gsap.utils.random(-400, 0);

            cube.position.set(x, y, z);

            gsap.to(cube.rotation, {
                x: Math.PI * 2,
                y: Math.PI * 2,
                z: Math.PI * 2,
                duration: gsap.utils.random(10, 20),
                repeat: -1,
                ease: 'none'
            });

            gsap.to(cube.position, {
                y: y + gsap.utils.random(-50, 50),
                duration: gsap.utils.random(3, 6),
                repeat: -1,
                yoyo: true,
                ease: 'sine.inOut'
            });

            this.scene.add(cube);
            this.objects.push(cube);
        }
    }

    /**
     * Create rings
     */
    createRings() {
        const ringCount = 5;
        const ringGeometry = new THREE.TorusGeometry(100, 2, 16, 100);

        for (let i = 0; i < ringCount; i++) {
            const material = new THREE.MeshPhongMaterial({
                color: [0x00d9ff, 0xff006e, 0x8f00ff][i % 3],
                emissive: [0x00d9ff, 0xff006e, 0x8f00ff][i % 3],
                emissiveIntensity: 0.3,
                wireframe: false,
                transparent: true,
                opacity: 0.3
            });

            const ring = new THREE.Mesh(ringGeometry, material);
            ring.rotation.x = Math.PI / (i + 1);
            ring.rotation.y = Math.PI / (i + 2);
            ring.position.z = -150;

            gsap.to(ring.rotation, {
                x: ring.rotation.x + Math.PI * 2,
                y: ring.rotation.y + Math.PI * 2,
                duration: 20 + i * 5,
                repeat: -1,
                ease: 'none'
            });

            this.scene.add(ring);
            this.objects.push(ring);
        }
    }

    /**
     * Create particle system
     */
    createParticles() {
        const particleCount = 1000;
        const geometry = new THREE.BufferGeometry();
        const positions = new Float32Array(particleCount * 3);
        const velocities = new Float32Array(particleCount * 3);

        for (let i = 0; i < particleCount * 3; i += 3) {
            positions[i] = gsap.utils.random(-300, 300);
            positions[i + 1] = gsap.utils.random(-300, 300);
            positions[i + 2] = gsap.utils.random(-400, 0);

            velocities[i] = gsap.utils.random(-0.5, 0.5);
            velocities[i + 1] = gsap.utils.random(-0.5, 0.5);
            velocities[i + 2] = gsap.utils.random(-0.5, 0.5);
        }

        geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
        geometry.setAttribute('velocity', new THREE.BufferAttribute(velocities, 3));

        const material = new THREE.PointsMaterial({
            color: 0x00d9ff,
            size: 0.5,
            transparent: true,
            opacity: 0.6,
            sizeAttenuation: true
        });

        this.particles = new THREE.Points(geometry, material);
        this.scene.add(this.particles);
    }

    /**
     * Handle mouse movement for interactive effects
     */
    onMouseMove(event) {
        this.targetMouse.x = (event.clientX / window.innerWidth) * 2 - 1;
        this.targetMouse.y = -(event.clientY / window.innerHeight) * 2 + 1;
    }

    /**
     * Update mouse position smoothly
     */
    updateMousePosition() {
        this.mouse.x += (this.targetMouse.x - this.mouse.x) * 0.05;
        this.mouse.y += (this.targetMouse.y - this.mouse.y) * 0.05;
    }

    /**
     * Update particle system
     */
    updateParticles() {
        const positions = this.particles.geometry.attributes.position.array;
        const velocities = this.particles.geometry.attributes.velocity.array;

        for (let i = 0; i < positions.length; i += 3) {
            positions[i] += velocities[i];
            positions[i + 1] += velocities[i + 1];
            positions[i + 2] += velocities[i + 2];

            // Wrap around
            if (positions[i] > 300) positions[i] = -300;
            if (positions[i] < -300) positions[i] = 300;
            if (positions[i + 1] > 300) positions[i + 1] = -300;
            if (positions[i + 1] < -300) positions[i + 1] = 300;
            if (positions[i + 2] > 0) positions[i + 2] = -400;
            if (positions[i + 2] < -400) positions[i + 2] = 0;
        }

        this.particles.geometry.attributes.position.needsUpdate = true;
    }

    /**
     * Main animation loop
     */
    animate = () => {
        requestAnimationFrame(this.animate);

        // Update mouse
        this.updateMousePosition();

        // Apply mouse influence to camera
        this.camera.position.x += (this.mouse.x * 50 - this.camera.position.x) * 0.05;
        this.camera.position.y += (this.mouse.y * 50 - this.camera.position.y) * 0.05;
        this.camera.lookAt(0, 0, -150);

        // Update particles
        this.updateParticles();

        // Render scene
        this.renderer.render(this.scene, this.camera);
    }

    /**
     * Handle window resize
     */
    onWindowResize() {
        const width = this.container.clientWidth;
        const height = this.container.clientHeight;

        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(width, height);
    }

    /**
     * Dispose of Three.js resources
     */
    dispose() {
        this.objects.forEach(obj => {
            if (obj.geometry) obj.geometry.dispose();
            if (obj.material) obj.material.dispose();
        });
        this.renderer.dispose();
    }
}

// Export for use
window.ThreeJSScene = ThreeJSScene;

/**
 * User & Permission Manager - Role-based access control & authentication
 * Handles user management, permissions, authentication tokens, and audit logs
 */

const crypto = require('crypto');
const EventEmitter = require('events');

class UserManager extends EventEmitter {
    constructor(database) {
        super();
        this.database = database;
        this.users = new Map();
        this.roles = new Map();
        this.permissions = new Map();
        this.sessions = new Map();
        this.setupDefaultRoles();
    }

    /**
     * Setup default roles and permissions
     */
    setupDefaultRoles() {
        // Define default roles
        this.defineRole('admin', 'Administrator with full access', [
            'system.manage',
            'users.manage',
            'processes.view',
            'processes.manage',
            'tasks.view',
            'tasks.manage',
            'logs.view',
            'logs.manage',
            'settings.view',
            'settings.manage'
        ]);

        this.defineRole('operator', 'System operator with limited control', [
            'system.view',
            'processes.view',
            'processes.pause',
            'tasks.view',
            'tasks.create',
            'logs.view',
            'settings.view'
        ]);

        this.defineRole('user', 'Regular user with basic access', [
            'system.view',
            'processes.view',
            'tasks.view',
            'tasks.create',
            'logs.view'
        ]);

        this.defineRole('guest', 'Guest with minimal access', [
            'system.view',
            'logs.view'
        ]);
    }

    /**
     * Define a new role
     */
    defineRole(roleName, description, permissions = []) {
        const role = {
            name: roleName,
            description,
            permissions,
            createdAt: new Date()
        };

        this.roles.set(roleName, role);
        this.emit('role-created', role);
        
        return role;
    }

    /**
     * Create a new user
     */
    createUser(username, password, email, role = 'user', metadata = {}) {
        if (this.users.has(username)) {
            throw new Error('User already exists');
        }

        const hashedPassword = this._hashPassword(password);
        const userId = crypto.randomUUID();

        const user = {
            userId,
            username,
            email,
            passwordHash: hashedPassword,
            role,
            status: 'active',
            createdAt: new Date(),
            lastLogin: null,
            loginCount: 0,
            metadata
        };

        this.users.set(username, user);
        this.emit('user-created', { userId, username, email, role });
        
        return {
            userId,
            username,
            email,
            role,
            status: user.status
        };
    }

    /**
     * Authenticate user
     */
    authenticateUser(username, password) {
        const user = this.users.get(username);
        if (!user) {
            throw new Error('User not found');
        }

        if (!this._verifyPassword(password, user.passwordHash)) {
            throw new Error('Invalid password');
        }

        if (user.status !== 'active') {
            throw new Error('User account is not active');
        }

        // Create session token
        const token = this._generateToken();
        const session = {
            token,
            userId: user.userId,
            username,
            role: user.role,
            createdAt: new Date(),
            expiresAt: new Date(Date.now() + 24*60*60*1000), // 24 hours
            lastActivity: new Date(),
            ip: null
        };

        this.sessions.set(token, session);

        // Update user login info
        user.lastLogin = new Date();
        user.loginCount++;

        this.emit('user-authenticated', { userId: user.userId, username });

        return {
            token,
            userId: user.userId,
            username,
            role: user.role,
            expiresAt: session.expiresAt
        };
    }

    /**
     * Validate session token
     */
    validateToken(token) {
        const session = this.sessions.get(token);
        
        if (!session) {
            return null;
        }

        if (session.expiresAt < new Date()) {
            this.sessions.delete(token);
            return null;
        }

        session.lastActivity = new Date();
        return session;
    }

    /**
     * Check user permission
     */
    hasPermission(username, permission) {
        const user = this.users.get(username);
        if (!user) return false;

        const role = this.roles.get(user.role);
        if (!role) return false;

        return role.permissions.includes(permission) || 
               role.permissions.includes(`${permission.split('.')[0]}.*`);
    }

    /**
     * Check multiple permissions (AND logic)
     */
    hasAllPermissions(username, permissions) {
        return permissions.every(perm => this.hasPermission(username, perm));
    }

    /**
     * Check multiple permissions (OR logic)
     */
    hasAnyPermission(username, permissions) {
        return permissions.some(perm => this.hasPermission(username, perm));
    }

    /**
     * Update user role
     */
    updateUserRole(username, newRole) {
        const user = this.users.get(username);
        if (!user) throw new Error('User not found');

        if (!this.roles.has(newRole)) throw new Error('Role not found');

        user.role = newRole;
        this.emit('user-role-updated', { username, newRole });
        
        return user;
    }

    /**
     * Disable user
     */
    disableUser(username) {
        const user = this.users.get(username);
        if (!user) throw new Error('User not found');

        user.status = 'disabled';
        
        // Invalidate all sessions
        for (const [token, session] of this.sessions.entries()) {
            if (session.username === username) {
                this.sessions.delete(token);
            }
        }

        this.emit('user-disabled', username);
        return user;
    }

    /**
     * Enable user
     */
    enableUser(username) {
        const user = this.users.get(username);
        if (!user) throw new Error('User not found');

        user.status = 'active';
        this.emit('user-enabled', username);
        
        return user;
    }

    /**
     * Logout user (invalidate token)
     */
    logout(token) {
        const session = this.sessions.get(token);
        if (session) {
            this.sessions.delete(token);
            this.emit('user-logout', { username: session.username });
        }
    }

    /**
     * Get user info
     */
    getUser(username) {
        const user = this.users.get(username);
        if (!user) return null;

        return {
            userId: user.userId,
            username: user.username,
            email: user.email,
            role: user.role,
            status: user.status,
            createdAt: user.createdAt,
            lastLogin: user.lastLogin,
            loginCount: user.loginCount
        };
    }

    /**
     * Get all users
     */
    getAllUsers() {
        return Array.from(this.users.values()).map(user => ({
            userId: user.userId,
            username: user.username,
            email: user.email,
            role: user.role,
            status: user.status
        }));
    }

    /**
     * Hash password
     */
    _hashPassword(password) {
        return crypto
            .createHash('sha256')
            .update(password + 'sa-aihos-salt')
            .digest('hex');
    }

    /**
     * Verify password
     */
    _verifyPassword(password, hash) {
        return this._hashPassword(password) === hash;
    }

    /**
     * Generate authentication token
     */
    _generateToken() {
        return crypto.randomBytes(32).toString('hex');
    }

    /**
     * Get statistics
     */
    getStatistics() {
        const activeSessions = Array.from(this.sessions.values())
            .filter(s => s.expiresAt > new Date());

        return {
            totalUsers: this.users.size,
            activeUsers: Array.from(this.users.values()).filter(u => u.status === 'active').length,
            disabledUsers: Array.from(this.users.values()).filter(u => u.status === 'disabled').length,
            totalRoles: this.roles.size,
            activeSessions: activeSessions.length,
            totalSessions: this.sessions.size
        };
    }
}

module.exports = UserManager;

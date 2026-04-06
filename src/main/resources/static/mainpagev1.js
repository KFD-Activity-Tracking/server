// API Configuration
const API_BASE_URL = 'http://localhost:8765';
let authToken = null;
let currentUserId = null;
let currentUsername = null;
let currentUserRole = null;
let mouseTrackingActive = false;
let keyboardTrackingActive = false;
let pendingKeyboardActions = []; // Store pending keyboard actions for bulk sending
let bulkSendInterval = null;
const BULK_SEND_DELAY = 5000; // Send every 5 seconds when in bulk mode
let bulkModeEnabled = false;

// DOM Elements
const authSection = document.getElementById('authSection');
const appSection = document.getElementById('appSection');
const loginBtn = document.getElementById('loginBtn');
const registerBtn = document.getElementById('registerBtn');
const logoutBtn = document.getElementById('logoutBtn');
const loadActionsBtn = document.getElementById('loadActionsBtn');
const submitActionBtn = document.getElementById('submitActionBtn');
const viewUserSelect = document.getElementById('viewUserSelect');
const actionsList = document.getElementById('actionsList');
const filterType = document.getElementById('filterType');
const currentUserSpan = document.getElementById('currentUser');

// Action type buttons
const actionTypeBtns = document.querySelectorAll('.action-type-btn');
const mouseForm = document.getElementById('mouseForm');
const keyboardForm = document.getElementById('keyboardForm');
const appForm = document.getElementById('appForm');

// Tracking elements
const startMouseTracking = document.getElementById('startMouseTracking');
const stopMouseTracking = document.getElementById('stopMouseTracking');
const mouseCoordinates = document.getElementById('mouseCoordinates');
const startKeyboardTracking = document.getElementById('startKeyboardTracking');
const stopKeyboardTracking = document.getElementById('stopKeyboardTracking');
const lastKeyPressed = document.getElementById('lastKeyPressed');

// Bulk mode controls
let bulkModeControls = `
    <div class="bulk-mode-controls" style="margin-top: 10px;">
        <label>
            <input type="checkbox" id="bulkModeCheckbox"> Send keyboard actions in bulk (every 5 seconds)
        </label>
        <button id="flushActionsBtn" style="margin-left: 10px; padding: 5px 10px; background: #ed8936;">Send Now</button>
    </div>
`;

// Auth tab switching
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        document.querySelectorAll('.auth-form').forEach(form => form.classList.remove('active'));
        document.getElementById(`${tab}Form`).classList.add('active');
    });
});

// Action type switching
actionTypeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        const type = btn.dataset.type;
        actionTypeBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        document.querySelectorAll('.action-form').forEach(form => form.classList.remove('active'));
        document.getElementById(`${type}Form`).classList.add('active');

        // Add bulk mode controls to keyboard form if not already added
        if (type === 'keyboard' && !document.querySelector('.bulk-mode-controls')) {
            const keyboardFormContainer = document.getElementById('keyboardForm');
            const existingControls = keyboardFormContainer.querySelector('.bulk-mode-controls');
            if (!existingControls) {
                keyboardFormContainer.insertAdjacentHTML('beforeend', bulkModeControls);
                setupBulkModeListeners();
            }
        }
    });
});

function setupBulkModeListeners() {
    const bulkModeCheckbox = document.getElementById('bulkModeCheckbox');
    const flushActionsBtn = document.getElementById('flushActionsBtn');

    if (bulkModeCheckbox) {
        bulkModeCheckbox.addEventListener('change', (e) => {
            bulkModeEnabled = e.target.checked;
            if (bulkModeEnabled) {
                startBulkSending();
            } else {
                stopBulkSending();
                // Send any pending actions immediately when disabling bulk mode
                if (pendingKeyboardActions.length > 0) {
                    sendKeyboardActionsInBulk();
                }
            }
        });
    }

    if (flushActionsBtn) {
        flushActionsBtn.addEventListener('click', () => {
            if (pendingKeyboardActions.length > 0) {
                sendKeyboardActionsInBulk();
                showActionMessage(`Sent ${pendingKeyboardActions.length} pending keyboard actions`, true);
            } else {
                showActionMessage('No pending actions to send', false);
            }
        });
    }
}

function startBulkSending() {
    if (bulkSendInterval) clearInterval(bulkSendInterval);
    bulkSendInterval = setInterval(() => {
        if (pendingKeyboardActions.length > 0) {
            sendKeyboardActionsInBulk();
        }
    }, BULK_SEND_DELAY);
}

function stopBulkSending() {
    if (bulkSendInterval) {
        clearInterval(bulkSendInterval);
        bulkSendInterval = null;
    }
}

// Helper Functions
function showError(elementId, message) {
    const errorElement = document.getElementById(elementId);
    errorElement.textContent = message;
}

function showActionMessage(message, isSuccess = true) {
    const messageDiv = document.getElementById('actionMessage');
    messageDiv.textContent = message;
    messageDiv.className = `action-message ${isSuccess ? 'success' : 'error'}`;
    setTimeout(() => {
        messageDiv.className = 'action-message';
        messageDiv.textContent = '';
    }, 8000);
}

// Store JWT token
function storeToken(token) {
    authToken = token;
    localStorage.setItem('jwt_token', token);
}

// Get stored token
function getStoredToken() {
    return localStorage.getItem('jwt_token');
}

// Make authenticated request
async function authenticatedRequest(url, options = {}) {
    const token = getStoredToken();
    if (!token) {
        throw new Error('No authentication token');
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers
    };

    const response = await fetch(`${API_BASE_URL}${url}`, {
        ...options,
        headers
    });

    if (response.status === 401 || response.status === 403) {
        logout();
        throw new Error('Session expired');
    }

    return response;
}

// Login function
async function login(username, password) {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Invalid credentials');
        }

        const data = await response.json();
        console.log('Login response:', data);

        if (data.token) {
            storeToken(data.token);
            await loadUserInfo();
            return true;
        } else {
            throw new Error('No token received');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError('loginError', error.message);
        return false;
    }
}

// Register function
async function register(username, password, role) {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password, role })
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Registration failed');
        }

        const data = await response.json();
        console.log('Register response:', data);

        if (data.token) {
            storeToken(data.token);
            await loadUserInfo();
            return true;
        } else {
            throw new Error('No token received');
        }
    } catch (error) {
        console.error('Registration error:', error);
        showError('registerError', error.message);
        return false;
    }
}

// Load current user info
async function loadUserInfo() {
    try {
        const response = await authenticatedRequest('/api/users/owninfo');

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const userInfo = await response.json();
        console.log('User info received:', userInfo);

        currentUserId = userInfo.userId;
        currentUsername = userInfo.username;
        currentUserRole = userInfo.role;
        currentUserSpan.textContent = `${currentUsername} (${currentUserRole})`;

        await loadAllUsers();

        authSection.style.display = 'none';
        appSection.style.display = 'block';
    } catch (error) {
        console.error('Failed to load user info:', error);
        showActionMessage('Failed to load user info: ' + error.message, false);
        logout();
    }
}

// Load all users for dropdown
async function loadAllUsers() {
    try {
        const response = await authenticatedRequest('/api/users/all');
        const users = await response.json();

        viewUserSelect.innerHTML = '<option value="">Select User</option>';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            option.textContent = `${user.username} (${user.role})`;
            viewUserSelect.appendChild(option);
        });

        viewUserSelect.value = currentUserId;
        await loadUserActions(currentUserId);
    } catch (error) {
        console.error('Failed to load users:', error);
    }
}

// Load actions for a specific user
async function loadUserActions(userId) {
    if (!userId) {
        actionsList.innerHTML = '<p>Please select a user</p>';
        return;
    }

    try {
        const response = await authenticatedRequest(`/api/actions/from/${userId}`);
        const actions = await response.json();
        displayActions(actions);
    } catch (error) {
        console.error('Failed to load actions:', error);
        actionsList.innerHTML = '<p class="error">Failed to load actions</p>';
    }
}

// Display actions with filtering
function displayActions(actions) {
    const filter = filterType.value;
    let filteredActions = actions;

    if (filter !== 'all') {
        filteredActions = actions.filter(action => action.type === filter);
    }

    if (filteredActions.length === 0) {
        actionsList.innerHTML = '<p>No actions found</p>';
        return;
    }

    actionsList.innerHTML = filteredActions.map(action => {
        let details = '';
        let actionClass = '';

        switch (action.type) {
            case 'mouse':
                actionClass = 'mouse';
                details = `Mouse Movement: Delta X = ${action.delta_x || 0}, Delta Y = ${action.delta_y || 0}`;
                break;
            case 'keyboard':
                actionClass = 'keyboard';
                details = `Key Press: Key Code = ${action.keyboard_key || 0}`;
                break;
            case 'app':
                actionClass = 'app';
                details = `App: ${action.app_name || 'Unknown'}`;
                break;
        }

        const date = new Date(action.performedAt);
        const formattedDate = date.toLocaleString();

        return `
            <div class="action-card ${actionClass}">
                <div class="action-header">
                    <span class="action-type-badge">${action.type.toUpperCase()}</span>
                    <span>${formattedDate}</span>
                </div>
                <div class="action-details">
                    ${details}
                </div>
                <div class="action-footer">
                    <small>Action ID: ${action.id}</small>
                </div>
            </div>
        `;
    }).join('');
}

// Create a complete action object with all required fields
function createActionObject(type, specificFields = {}) {
    // Get current timestamp in ISO format
    const now = new Date();
    const performedAt = now.toISOString();

    // Base action object with all required fields
    const action = {
        type: type,
        performedAt: performedAt,
        // Note: user field will be set by the backend based on the JWT token
        // We don't need to send it from the frontend
        ...specificFields
    };

    // Add type-specific fields
    switch (type) {
        case 'mouse':
            action.delta_x = specificFields.delta_x || 0;
            action.delta_y = specificFields.delta_y || 0;
            break;
        case 'keyboard':
            action.keyboard_key = specificFields.keyboard_key || 0;
            break;
        case 'app':
            action.app_name = specificFields.app_name || '';
            break;
    }

    console.log('Created action object:', action);
    return action;
}

// Submit a single keyboard action (immediate mode)
async function submitKeyboardActionImmediate(keyCode) {
    const action = createActionObject('keyboard', {
        keyboard_key: keyCode
    });

    return await sendActions([action]);
}

// Add keyboard action to pending bulk list
function addKeyboardActionToBulk(keyCode) {
    const action = createActionObject('keyboard', {
        keyboard_key: keyCode
    });
    pendingKeyboardActions.push(action);
    console.log(`Added keyboard action to bulk. Total pending: ${pendingKeyboardActions.length}`);
    showActionMessage(`Key recorded (${pendingKeyboardActions.length} pending)`, true);
}

// Send keyboard actions in bulk
async function sendKeyboardActionsInBulk() {
    if (pendingKeyboardActions.length === 0) return;

    const actionsToSend = [...pendingKeyboardActions];
    pendingKeyboardActions = []; // Clear immediately to avoid duplicates

    try {
        await sendActions(actionsToSend);
        console.log(`Sent ${actionsToSend.length} keyboard actions in bulk`);
        showActionMessage(`Sent ${actionsToSend.length} keyboard actions`, true);
    } catch (error) {
        console.error('Failed to send bulk actions:', error);
        // Re-add failed actions to pending queue
        pendingKeyboardActions.unshift(...actionsToSend);
        showActionMessage('Failed to send bulk actions', false);
    }
}

// Send actions to backend
async function sendActions(actions) {
    console.log('Sending actions to backend:', JSON.stringify(actions, null, 2));

    const response = await authenticatedRequest('/api/actions/addAll', {
        method: 'POST',
        body: JSON.stringify(actions)
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to send actions: ${response.status} - ${errorText}`);
    }

    const result = await response.json();
    console.log('Actions saved successfully:', result);
    return result;
}

// Submit a new action
async function submitAction() {
    const activeType = document.querySelector('.action-type-btn.active').dataset.type;

    if (activeType === 'keyboard') {
        const keyCode = parseInt(document.getElementById('keyCode').value) || 0;
        if (!keyCode) {
            showActionMessage('Please enter a key code or use real-time tracking', false);
            return;
        }

        if (bulkModeEnabled) {
            addKeyboardActionToBulk(keyCode);
            document.getElementById('keyCode').value = ''; // Clear for next input
        } else {
            await submitKeyboardActionImmediate(keyCode);
            showActionMessage('Keyboard action added successfully!', true);
            document.getElementById('keyCode').value = '';
        }

        // Reload actions
        await loadUserActions(viewUserSelect.value);
        return;
    }

    // Handle mouse and app actions
    let specificFields = {};

    switch (activeType) {
        case 'mouse':
            const deltaX = parseInt(document.getElementById('mouseDeltaX').value) || 0;
            const deltaY = parseInt(document.getElementById('mouseDeltaY').value) || 0;
            specificFields = {
                delta_x: deltaX,
                delta_y: deltaY
            };
            break;
        case 'app':
            const appName = document.getElementById('appName').value;
            if (!appName) {
                showActionMessage('Please enter an app name', false);
                return;
            }
            specificFields = {
                app_name: appName
            };
            break;
    }

    const action = createActionObject(activeType, specificFields);

    try {
        await sendActions([action]);
        showActionMessage('Action added successfully!', true);

        // Clear forms
        document.getElementById('mouseDeltaX').value = '';
        document.getElementById('mouseDeltaY').value = '';
        document.getElementById('appName').value = '';

        // Reload actions
        await loadUserActions(viewUserSelect.value);
    } catch (error) {
        console.error('Failed to add action:', error);
        showActionMessage('Failed to add action: ' + error.message, false);
    }
}

// Mouse tracking
function startMouseTrackingHandler() {
    if (mouseTrackingActive) return;
    mouseTrackingActive = true;
    startMouseTracking.disabled = true;
    stopMouseTracking.disabled = false;

    document.addEventListener('mousemove', trackMouseMove);
}

function stopMouseTrackingHandler() {
    mouseTrackingActive = false;
    startMouseTracking.disabled = false;
    stopMouseTracking.disabled = true;
    document.removeEventListener('mousemove', trackMouseMove);
    mouseCoordinates.textContent = 'X: 0, Y: 0';
}

function trackMouseMove(e) {
    mouseCoordinates.textContent = `X: ${e.clientX}, Y: ${e.clientY}`;
}

// Keyboard tracking
function startKeyboardTrackingHandler() {
    if (keyboardTrackingActive) return;
    keyboardTrackingActive = true;
    startKeyboardTracking.disabled = true;
    stopKeyboardTracking.disabled = false;

    document.addEventListener('keydown', trackKeyPress);
}

function stopKeyboardTrackingHandler() {
    keyboardTrackingActive = false;
    startKeyboardTracking.disabled = false;
    stopKeyboardTracking.disabled = true;
    document.removeEventListener('keydown', trackKeyPress);
    lastKeyPressed.textContent = 'Last key: None';
}

async function trackKeyPress(e) {
    // Prevent tracking of modifier keys only
    if (e.key === 'Shift' || e.key === 'Control' || e.key === 'Alt' || e.key === 'Meta') {
        return;
    }

    // Prevent default to avoid triggering browser shortcuts while tracking
    // Comment this out if you want to keep browser shortcuts
    // e.preventDefault();

    const keyCode = e.keyCode || e.which;
    const key = e.key;

    lastKeyPressed.textContent = `Last key: ${key} (Code: ${keyCode})`;

    // Auto-fill key code field
    document.getElementById('keyCode').value = keyCode;

    // Send or queue the keyboard action based on mode
    if (bulkModeEnabled) {
        addKeyboardActionToBulk(keyCode);
    } else {
        await submitKeyboardActionImmediate(keyCode);
        showActionMessage(`Key "${key}" recorded!`, true);
        // Reload actions to show the new one
        await loadUserActions(viewUserSelect.value);
    }
}

// Logout function
function logout() {
    localStorage.removeItem('jwt_token');
    authToken = null;
    currentUserId = null;
    currentUsername = null;

    // Stop bulk sending
    stopBulkSending();
    pendingKeyboardActions = [];

    authSection.style.display = 'block';
    appSection.style.display = 'none';

    // Clear forms
    document.getElementById('loginUsername').value = '';
    document.getElementById('loginPassword').value = '';
    document.getElementById('regUsername').value = '';
    document.getElementById('regPassword').value = '';
    document.getElementById('regRole').value = 'USER';

    // Stop tracking if active
    if (mouseTrackingActive) stopMouseTrackingHandler();
    if (keyboardTrackingActive) stopKeyboardTrackingHandler();
}

// Event Listeners
loginBtn.addEventListener('click', async () => {
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    if (username && password) {
        await login(username, password);
    } else {
        showError('loginError', 'Please enter username and password');
    }
});

registerBtn.addEventListener('click', async () => {
    const username = document.getElementById('regUsername').value;
    const password = document.getElementById('regPassword').value;
    const role = document.getElementById('regRole').value;
    if (username && password) {
        await register(username, password, role);
    } else {
        showError('registerError', 'Please enter username and password');
    }
});

logoutBtn.addEventListener('click', logout);

loadActionsBtn.addEventListener('click', () => {
    const userId = viewUserSelect.value;
    if (userId) {
        loadUserActions(userId);
    } else {
        showActionMessage('Please select a user', false);
    }
});

submitActionBtn.addEventListener('click', submitAction);

filterType.addEventListener('change', () => {
    const userId = viewUserSelect.value;
    if (userId) {
        loadUserActions(userId);
    }
});

// Tracking event listeners
startMouseTracking.addEventListener('click', startMouseTrackingHandler);
stopMouseTracking.addEventListener('click', stopMouseTrackingHandler);
startKeyboardTracking.addEventListener('click', startKeyboardTrackingHandler);
stopKeyboardTracking.addEventListener('click', stopKeyboardTrackingHandler);

// Check for existing token on page load
window.addEventListener('load', () => {
    const token = getStoredToken();
    if (token) {
        authToken = token;
        loadUserInfo().catch(() => {
            localStorage.removeItem('jwt_token');
        });
    }
});

// Enter key support for login/register
document.getElementById('loginPassword').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') loginBtn.click();
});

document.getElementById('regPassword').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') registerBtn.click();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (bulkModeEnabled && pendingKeyboardActions.length > 0) {
        // Try to send pending actions before closing
        sendKeyboardActionsInBulk();
    }
});
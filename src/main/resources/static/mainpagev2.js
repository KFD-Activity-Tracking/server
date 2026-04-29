// API Configuration
const API_BASE_URL = 'http://localhost:8765';
let authToken = null;
let currentUserId = null;
let currentUsername = null;
let currentUserRole = null;
let currentRealName = null;
let mouseTrackingActive = false;
let keyboardTrackingActive = false;
let pendingKeyboardActions = [];
let pendingMouseActions = [];
let bulkSendInterval = null;
let mouseTrackingInterval = null;
const BULK_SEND_DELAY = 5000;
const MOUSE_TRACKING_INTERVAL = 200; // 5 actions per second (200ms interval)
const MOUSE_MOVEMENT_THRESHOLD = 5; // Minimum movement to record
let bulkModeEnabled = false;
let lastMouseX = 0;
let lastMouseY = 0;
let mouseActionsSent = 0;

let lastMenuError = null;

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
const menuLinks = document.getElementById('menuLinks');

// Message elements
const loginError = document.getElementById('loginError');
const registerError = document.getElementById('registerError');
const actionMessage = document.getElementById('actionMessage');

// Action type buttons & forms
const actionTypeBtns = document.querySelectorAll('.action-type-btn');
const mouseForm = document.getElementById('mouseForm');
const keyboardForm = document.getElementById('keyboardForm');
const appForm = document.getElementById('appForm');

// Tracking elements
const startMouseTracking = document.getElementById('startMouseTracking');
const stopMouseTracking = document.getElementById('stopMouseTracking');
const mouseCoordinates = document.getElementById('mouseCoordinates');
const trackingStats = document.getElementById('trackingStats');
const startKeyboardTracking = document.getElementById('startKeyboardTracking');
const stopKeyboardTracking = document.getElementById('stopKeyboardTracking');
const lastKeyPressed = document.getElementById('lastKeyPressed');

// Edit name modal elements
const editRealNameBtn = document.getElementById('editRealNameBtn');
const editNameModal = document.getElementById('editNameModal');
const editRealNameInput = document.getElementById('editRealNameInput');
const saveRealNameBtn = document.getElementById('saveRealNameBtn');
const editNameError = document.getElementById('editNameError');
const modalClose = document.querySelector('.modal-close');

// ========================
//  Helper Functions
// ========================
function showError(elementId, message) {
    const el = document.getElementById(elementId);
    if (el) el.textContent = message;
}

function showActionMessage(message, isSuccess = true) {
    actionMessage.textContent = message;
    actionMessage.className = `action-message ${isSuccess ? 'success' : 'error'}`;
    setTimeout(() => {
        if (actionMessage.textContent === message) {
            clearActionMessage();
        }
    }, 3000);
}

function clearActionMessage() {
    actionMessage.textContent = '';
    actionMessage.className = 'action-message';
}

function storeToken(token) {
    authToken = token;
    localStorage.setItem('jwt_token', token);
}

function getStoredToken() {
    return localStorage.getItem('jwt_token');
}

async function authenticatedRequest(url, options = {}) {
    const token = getStoredToken();
    if (!token) throw new Error('No authentication token');

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

// ========================
//  Menu Functions
// ========================
async function loadMenuItems() {
    const menuLinksDiv = document.getElementById('menuLinks');
    const errorDetailsDiv = document.getElementById('menuErrorDetails');
    try {
        const response = await authenticatedRequest('/api/info/pages');
        if (response.ok) {
            const pages = await response.json();
            menuLinksDiv.innerHTML = '';
            pages.forEach((page) => {
                const link = document.createElement('a');
                link.href = page.second;
                link.textContent = page.first;
                link.target = '_blank';
                link.className = 'menu-link';
                menuLinksDiv.appendChild(link);
            });
            lastMenuError = null;
            errorDetailsDiv.style.display = 'none';
            errorDetailsDiv.textContent = '';
        } else {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }
    } catch (error) {
        console.error('Error loading menu:', error);
        lastMenuError = error.message;
        menuLinksDiv.innerHTML = '<span class="menu-error">⚠️ Menu unavailable</span>';
        errorDetailsDiv.style.display = 'none';
    }
}

function showMenuErrorDetails() {
    const errorDetailsDiv = document.getElementById('menuErrorDetails');
    if (lastMenuError) {
        errorDetailsDiv.textContent = `Error: ${lastMenuError}`;
        errorDetailsDiv.style.display = 'block';
    } else {
        errorDetailsDiv.textContent = 'No error. Menu loaded successfully.';
        errorDetailsDiv.style.display = 'block';
        setTimeout(() => {
            errorDetailsDiv.style.display = 'none';
        }, 3000);
    }
}

async function refreshMenu() {
    const menuLinksDiv = document.getElementById('menuLinks');
    menuLinksDiv.innerHTML = '<span class="menu-loading">Loading menu...</span>';
    await loadMenuItems();
}

// ========================
//  Real Name Management
// ========================
async function updateRealName(newRealName) {
    try {
        const response = await authenticatedRequest('/api/users/updateRealName', {
            method: 'PUT',
            body: JSON.stringify({ realName: newRealName })
        });
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to update name');
        }
        currentRealName = newRealName;
        updateUserDisplay();
        showActionMessage('Real name updated successfully!', true);
        return true;
    } catch (error) {
        showActionMessage('Failed to update real name: ' + error.message, false);
        return false;
    }
}

function updateUserDisplay() {
    const nameDisplay = currentRealName && currentRealName !== currentUsername
        ? `${currentUsername} (${currentRealName}) - ${currentUserRole}`
        : `${currentUsername} (${currentUserRole})`;
    currentUserSpan.textContent = nameDisplay;
}

// ========================
//  Auth & User
// ========================
async function login(username, password) {
    try {
        clearActionMessage();
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Invalid credentials');
        }
        const data = await response.json();
        if (data.token) {
            storeToken(data.token);
            await loadUserInfo();
            return true;
        }
        throw new Error('No token received');
    } catch (error) {
        showError('loginError', error.message);
        return false;
    }
}

async function register(username, realName, password, role) {
    try {
        clearActionMessage();
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, realName, password, role })
        });
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Registration failed');
        }
        const data = await response.json();
        if (data.token) {
            storeToken(data.token);
            await loadUserInfo();
            return true;
        }
        throw new Error('No token received');
    } catch (error) {
        showError('registerError', error.message);
        return false;
    }
}

async function loadUserInfo() {
    try {
        const response = await authenticatedRequest('/api/users/owninfo');
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const userInfo = await response.json();
        currentUserId = userInfo.id;
        currentUsername = userInfo.username;
        currentUserRole = userInfo.role;
        currentRealName = userInfo.realName;
        updateUserDisplay();

        await loadAllUsers();

        authSection.style.display = 'none';
        appSection.style.display = 'block';
        await loadMenuItems();
    } catch (error) {
        console.error('loadUserInfo error:', error);
        showActionMessage('Failed to load user info: ' + error.message, false);
        logout();
    }
}

async function loadAllUsers() {
    try {
        const response = await authenticatedRequest('/api/users/all');
        const users = await response.json();
        viewUserSelect.innerHTML = '<option value="">Select User</option>';
        users.forEach(user => {
            const option = document.createElement('option');
            option.value = user.id;
            const displayName = user.realName && user.realName !== user.username
                ? `${user.username} (${user.realName}) - ${user.role}`
                : `${user.username} - ${user.role}`;
            option.textContent = displayName;
            viewUserSelect.appendChild(option);
        });
        viewUserSelect.value = currentUserId;
        await loadUserActions(currentUserId);
    } catch (error) {
        showActionMessage('Failed to load users: ' + error.message, false);
    }
}

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
        actionsList.innerHTML = '<p class="error">Failed to load actions</p>';
        showActionMessage('Failed to load actions: ' + error.message, false);
    }
}

function displayActions(actions) {
    const filter = filterType.value;
    let filteredActions = actions;
    if (filter !== 'all') {
        filteredActions = actions.filter(action => {
            if (filter === 'mouse') return action.type === 'mouse';
            if (filter === 'keyboard') return action.type === 'keyboard';
            if (filter === 'app') return action.type === 'app';
            return false;
        });
    }
    if (filteredActions.length === 0) {
        actionsList.innerHTML = '<p>No actions found</p>';
        return;
    }
    actionsList.innerHTML = filteredActions.map(action => {
        let details = '', actionClass = '', type = '';
        if (action.type === 'mouse') {
            type = 'mouse'; actionClass = 'mouse';
            details = `Mouse: Delta X = ${(action.delta_x || 0).toFixed(2)}, Delta Y = ${(action.delta_y || 0).toFixed(2)}`;
            if (action.is_click) details += ` | 🖱️ CLICK`;
        } else if (action.type === 'keyboard') {
            type = 'keyboard'; actionClass = 'keyboard';
            details = `Key Press: Key Code = ${action.keyboard_key || 0}`;
        } else if (action.type === 'app') {
            type = 'app'; actionClass = 'app';
            details = `App: ${action.app_name || 'Unknown'}`;
        } else {
            type = 'unknown'; actionClass = 'unknown';
            details = 'Unknown action type';
        }
        const date = new Date(action.performedAt);
        const formattedDate = date.toLocaleString();
        return `
            <div class="action-card ${actionClass}">
                <div class="action-header">
                    <span class="action-type-badge">${type.toUpperCase()}</span>
                    <span>${formattedDate}</span>
                </div>
                <div class="action-details">${details}</div>
                <div class="action-footer"><small>Action ID: ${action.id}</small></div>
            </div>
        `;
    }).join('');
}

// ========================
//  Action Creation (with 'type' field)
// ========================
function createActionObject(type, specificFields = {}) {
    const performedAt = new Date().toISOString();
    return { type, performedAt, ...specificFields };
}

async function sendActions(actions) {
    console.log('Sending actions:', JSON.stringify(actions, null, 2));
    const response = await authenticatedRequest('/api/actions/addAll', {
        method: 'POST',
        body: JSON.stringify(actions)
    });
    if (!response.ok) {
        let errorMsg;
        try {
            const err = await response.json();
            errorMsg = err.detail || err.message || `HTTP ${response.status}`;
        } catch {
            errorMsg = await response.text();
        }
        throw new Error(errorMsg);
    }
    const text = await response.text();
    return text ? JSON.parse(text) : {};
}

async function submitMouseActionImmediate(deltaX, deltaY, isClick) {
    const action = createActionObject('mouse', { delta_x: deltaX, delta_y: deltaY, is_click: isClick });
    return await sendActions([action]);
}

async function submitKeyboardActionImmediate(keyCode) {
    const action = createActionObject('keyboard', { keyboard_key: keyCode });
    return await sendActions([action]);
}

function addKeyboardActionToBulk(keyCode) {
    const action = createActionObject('keyboard', { keyboard_key: keyCode });
    pendingKeyboardActions.push(action);
    showActionMessage(`Key recorded (${pendingKeyboardActions.length} pending)`, true);
}

async function sendKeyboardActionsInBulk() {
    if (pendingKeyboardActions.length === 0) return;
    const actionsToSend = [...pendingKeyboardActions];
    pendingKeyboardActions = [];
    try {
        await sendActions(actionsToSend);
        showActionMessage(`Sent ${actionsToSend.length} keyboard actions`, true);
    } catch (error) {
        pendingKeyboardActions.unshift(...actionsToSend);
        showActionMessage('Failed to send bulk actions: ' + error.message, false);
    }
}

async function sendMouseActionFromTracking(deltaX, deltaY, isClick) {
    try {
        await submitMouseActionImmediate(deltaX, deltaY, isClick);
        mouseActionsSent++;
        if (trackingStats) {
            trackingStats.textContent = `Actions sent: ${mouseActionsSent}`;
        }
    } catch (error) {
        console.error('Failed to send mouse action:', error);
    }
}

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
            document.getElementById('keyCode').value = '';
        } else {
            await submitKeyboardActionImmediate(keyCode);
            showActionMessage('Keyboard action added successfully!', true);
            document.getElementById('keyCode').value = '';
        }
        await loadUserActions(viewUserSelect.value);
        return;
    }

    let specificFields = {};
    if (activeType === 'mouse') {
        const deltaX = parseFloat(document.getElementById('mouseDeltaX').value) || 0;
        const deltaY = parseFloat(document.getElementById('mouseDeltaY').value) || 0;
        const isClick = document.getElementById('isClick').checked;
        specificFields = { delta_x: deltaX, delta_y: deltaY, is_click: isClick };
    } else if (activeType === 'app') {
        const appName = document.getElementById('appName').value;
        if (!appName) {
            showActionMessage('Please enter an app name', false);
            return;
        }
        specificFields = { app_name: appName };
    }

    const action = createActionObject(activeType, specificFields);
    try {
        await sendActions([action]);
        showActionMessage('Action added successfully!', true);
        document.getElementById('mouseDeltaX').value = '';
        document.getElementById('mouseDeltaY').value = '';
        document.getElementById('isClick').checked = false;
        document.getElementById('appName').value = '';
        await loadUserActions(viewUserSelect.value);
    } catch (error) {
        showActionMessage('Failed to add action: ' + error.message, false);
    }
}

// ========================
//  Mouse Tracking with Throttling
// ========================
function startMouseTrackingHandler() {
    if (mouseTrackingActive) return;
    mouseTrackingActive = true;
    startMouseTracking.disabled = true;
    stopMouseTracking.disabled = false;
    mouseActionsSent = 0;
    if (trackingStats) {
        trackingStats.textContent = 'Actions sent: 0';
    }

    let accumulatedDeltaX = 0;
    let accumulatedDeltaY = 0;
    let lastSendX = 0;
    let lastSendY = 0;
    let lastTimestamp = Date.now();

    const mouseMoveHandler = (e) => {
        if (!mouseTrackingActive) return;

        const currentX = e.clientX;
        const currentY = e.clientY;

        if (lastSendX === 0 && lastSendY === 0) {
            lastSendX = currentX;
            lastSendY = currentY;
        }

        let deltaX = currentX - lastSendX;
        let deltaY = currentY - lastSendY;

        if (Math.abs(deltaX) > MOUSE_MOVEMENT_THRESHOLD || Math.abs(deltaY) > MOUSE_MOVEMENT_THRESHOLD) {
            accumulatedDeltaX += deltaX;
            accumulatedDeltaY += deltaY;
            lastSendX = currentX;
            lastSendY = currentY;
        }

        mouseCoordinates.textContent = `X: ${currentX}, Y: ${currentY} | Acc: Δ${accumulatedDeltaX.toFixed(1)}, Δ${accumulatedDeltaY.toFixed(1)}`;
    };

    const sendAccumulatedMovement = () => {
        if (!mouseTrackingActive) return;

        const now = Date.now();
        if (now - lastTimestamp >= MOUSE_TRACKING_INTERVAL) {
            if (Math.abs(accumulatedDeltaX) > 0.1 || Math.abs(accumulatedDeltaY) > 0.1) {
                sendMouseActionFromTracking(accumulatedDeltaX, accumulatedDeltaY, false);
                accumulatedDeltaX = 0;
                accumulatedDeltaY = 0;
            }
            lastTimestamp = now;
        }
    };

    document.addEventListener('mousemove', mouseMoveHandler);
    mouseTrackingInterval = setInterval(sendAccumulatedMovement, MOUSE_TRACKING_INTERVAL);

    // Store handlers for cleanup
    window._mouseMoveHandler = mouseMoveHandler;

    showActionMessage('Mouse tracking started (sending ~5 actions/sec with threshold)', true);
}

function stopMouseTrackingHandler() {
    mouseTrackingActive = false;
    startMouseTracking.disabled = false;
    stopMouseTracking.disabled = true;

    if (window._mouseMoveHandler) {
        document.removeEventListener('mousemove', window._mouseMoveHandler);
        window._mouseMoveHandler = null;
    }
    if (mouseTrackingInterval) {
        clearInterval(mouseTrackingInterval);
        mouseTrackingInterval = null;
    }

    mouseCoordinates.textContent = 'X: 0, Y: 0';
    if (trackingStats) {
        trackingStats.textContent = `Actions sent: ${mouseActionsSent} (Final)`;
    }
    showActionMessage(`Mouse tracking stopped. Sent ${mouseActionsSent} actions.`, true);
}

// ========================
//  Keyboard Tracking
// ========================
function startKeyboardTrackingHandler() {
    if (keyboardTrackingActive) return;
    keyboardTrackingActive = true;
    startKeyboardTracking.disabled = true;
    stopKeyboardTracking.disabled = false;
    document.addEventListener('keydown', trackKeyPress);
    showActionMessage('Keyboard tracking started', true);
}

function stopKeyboardTrackingHandler() {
    keyboardTrackingActive = false;
    startKeyboardTracking.disabled = false;
    stopKeyboardTracking.disabled = true;
    document.removeEventListener('keydown', trackKeyPress);
    lastKeyPressed.textContent = 'Last key: None';
    showActionMessage('Keyboard tracking stopped', true);
}

async function trackKeyPress(e) {
    if (['Shift', 'Control', 'Alt', 'Meta'].includes(e.key)) return;
    const keyCode = e.keyCode || e.which;
    const key = e.key;
    lastKeyPressed.textContent = `Last key: ${key} (Code: ${keyCode})`;
    document.getElementById('keyCode').value = keyCode;
    if (bulkModeEnabled) {
        addKeyboardActionToBulk(keyCode);
    } else {
        await submitKeyboardActionImmediate(keyCode);
        showActionMessage(`Key "${key}" recorded!`, true);
        await loadUserActions(viewUserSelect.value);
    }
}

// ========================
//  Bulk Mode
// ========================
function startBulkSending() {
    if (bulkSendInterval) clearInterval(bulkSendInterval);
    bulkSendInterval = setInterval(() => {
        if (pendingKeyboardActions.length) sendKeyboardActionsInBulk();
    }, BULK_SEND_DELAY);
}

function stopBulkSending() {
    if (bulkSendInterval) {
        clearInterval(bulkSendInterval);
        bulkSendInterval = null;
    }
}

function setupBulkModeListeners() {
    const cb = document.getElementById('bulkModeCheckbox');
    const flushBtn = document.getElementById('flushActionsBtn');
    if (cb) {
        cb.addEventListener('change', (e) => {
            bulkModeEnabled = e.target.checked;
            if (bulkModeEnabled) startBulkSending();
            else {
                stopBulkSending();
                if (pendingKeyboardActions.length) sendKeyboardActionsInBulk();
                showActionMessage('Bulk mode disabled', true);
            }
        });
    }
    if (flushBtn) {
        flushBtn.addEventListener('click', () => {
            if (pendingKeyboardActions.length) sendKeyboardActionsInBulk();
            else showActionMessage('No pending actions', false);
        });
    }
}

// ========================
//  Logout
// ========================
function logout() {
    localStorage.removeItem('jwt_token');
    authToken = null;
    stopBulkSending();
    if (mouseTrackingActive) stopMouseTrackingHandler();
    if (keyboardTrackingActive) stopKeyboardTrackingHandler();
    if (mouseTrackingInterval) clearInterval(mouseTrackingInterval);
    pendingKeyboardActions = [];
    pendingMouseActions = [];
    authSection.style.display = 'block';
    appSection.style.display = 'none';
    loginError.textContent = '';
    registerError.textContent = '';
    actionMessage.textContent = '';
    actionMessage.className = 'action-message';
    menuLinks.innerHTML = '';
    document.getElementById('loginUsername').value = '';
    document.getElementById('loginPassword').value = '';
    document.getElementById('regUsername').value = '';
    document.getElementById('regRealName').value = '';
    document.getElementById('regPassword').value = '';
    document.getElementById('regRole').value = 'USER';
}

// ========================
//  Event Listeners & Init
// ========================
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        document.querySelectorAll('.auth-form').forEach(f => f.classList.remove('active'));
        document.getElementById(`${tab}Form`).classList.add('active');
        loginError.textContent = '';
        registerError.textContent = '';
    });
});

actionTypeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        const type = btn.dataset.type;
        actionTypeBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        document.querySelectorAll('.action-form').forEach(f => f.classList.remove('active'));
        document.getElementById(`${type}Form`).classList.add('active');
        actionMessage.textContent = '';
        actionMessage.className = 'action-message';
    });
});

loginBtn.addEventListener('click', async () => {
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    if (username && password) await login(username, password);
    else showError('loginError', 'Please enter username and password');
});

registerBtn.addEventListener('click', async () => {
    const username = document.getElementById('regUsername').value;
    const realName = document.getElementById('regRealName').value;
    const password = document.getElementById('regPassword').value;
    const role = document.getElementById('regRole').value;
    if (username && password) await register(username, realName || username, password, role);
    else showError('registerError', 'Please enter username and password');
});

logoutBtn.addEventListener('click', logout);
loadActionsBtn.addEventListener('click', () => {
    const userId = viewUserSelect.value;
    if (userId) loadUserActions(userId);
    else showActionMessage('Please select a user', false);
});
submitActionBtn.addEventListener('click', submitAction);
filterType.addEventListener('change', () => {
    if (viewUserSelect.value) loadUserActions(viewUserSelect.value);
});
startMouseTracking.addEventListener('click', startMouseTrackingHandler);
stopMouseTracking.addEventListener('click', stopMouseTrackingHandler);
startKeyboardTracking.addEventListener('click', startKeyboardTrackingHandler);
stopKeyboardTracking.addEventListener('click', stopKeyboardTrackingHandler);

document.getElementById('refreshMenuBtn')?.addEventListener('click', refreshMenu);
document.getElementById('menuErrorBtn')?.addEventListener('click', showMenuErrorDetails);

// Edit name modal handlers
if (editRealNameBtn) {
    editRealNameBtn.addEventListener('click', () => {
        editRealNameInput.value = currentRealName || currentUsername;
        editNameModal.style.display = 'flex';
        editNameError.textContent = '';
    });
}

if (modalClose) {
    modalClose.addEventListener('click', () => {
        editNameModal.style.display = 'none';
    });
}

if (saveRealNameBtn) {
    saveRealNameBtn.addEventListener('click', async () => {
        const newName = editRealNameInput.value.trim();
        if (newName) {
            const success = await updateRealName(newName);
            if (success) {
                editNameModal.style.display = 'none';
                await loadAllUsers(); // Refresh user list
            }
        } else {
            editNameError.textContent = 'Please enter a valid name';
        }
    });
}

window.addEventListener('click', (e) => {
    if (e.target === editNameModal) {
        editNameModal.style.display = 'none';
    }
});

window.addEventListener('load', () => {
    const token = getStoredToken();
    if (token) loadUserInfo().catch(() => localStorage.removeItem('jwt_token'));
    setupBulkModeListeners();
});

document.getElementById('loginPassword')?.addEventListener('keypress', e => e.key === 'Enter' && loginBtn.click());
document.getElementById('regPassword')?.addEventListener('keypress', e => e.key === 'Enter' && registerBtn.click());

window.addEventListener('beforeunload', () => {
    if (bulkModeEnabled && pendingKeyboardActions.length) sendKeyboardActionsInBulk();
    if (mouseTrackingActive) stopMouseTrackingHandler();
});
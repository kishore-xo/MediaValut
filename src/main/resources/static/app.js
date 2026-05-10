// ─── Helpers ───
const $ = id => document.getElementById(id);
const ls = (k, v) => v !== undefined ? localStorage.setItem(k, v) : localStorage.getItem(k);
const lsRm = k => localStorage.removeItem(k);

// ─── Constants ───
const SK = { jwt: 'a0.jwt', uid: 'a0.userId', uname: 'a0.username', apikey: 'a0.apiKey' };
let currentPage = 'dashboard';
let activeQuality = 'source';
let currentObjectUrl = null;
let switchController = null;
let switchVersion = 0;
let currentVideoId = null;
let currentPhotoPreviewUrl = null;
let publicSocket = null;
let privateSocket = null;
let publicReconnectTimer = null;
let privateReconnectTimer = null;
let activeContact = null;
let chatHistory = JSON.parse(ls('a0.chatHistory') || '{}');
let contactList = JSON.parse(ls('a0.contactList') || '[]');

function toast(msg, type = 'info') {
    const c = $('toasts');
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = `<span>${type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ'}</span> ${msg}`;
    c.appendChild(t);
    setTimeout(() => { t.style.opacity = '0'; setTimeout(() => t.remove(), 300); }, 3500);
}

function getToken() {
    const t = ($('jwtToken')?.value || '').trim() || ls(SK.jwt) || '';
    if (t) { ls(SK.jwt, t); if ($('jwtToken')) $('jwtToken').value = t; }
    return t;
}

function getApiKey() {
    const v = ($('apiKey')?.value || '').trim() || ls(SK.apikey) ||"";
    if ($('apiKey')) $('apiKey').value = v;
    ls(SK.apikey, v);
    return v;
}

function authHeaders(optional = false) {
    const t = getToken();
    if (!t) { if (optional) return {}; throw new Error('JWT token required. Please log in.'); }
    return { Authorization: `Bearer ${t}` };
}

function withApiKey(url) {
    const u = new URL(url, location.origin);
    const k = getApiKey();
    if (k) u.searchParams.set('apikey', k);
    return u.toString();
}

function saveContext(uid, uname) {
    if (uid) { ls(SK.uid, String(uid)); if ($('userId')) $('userId').value = uid; }
    if (uname) ls(SK.uname, uname);
}

// ─── Navigation ───
function navigate(page) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    const el = $('page-' + page);
    const nav = $('nav-' + page);
    if (el) el.classList.add('active');
    if (nav) nav.classList.add('active');
    currentPage = page;
    // Close mobile sidebar
    document.querySelector('.sidebar')?.classList.remove('open');
    document.querySelector('.sidebar-overlay')?.classList.remove('open');

    if (page === 'videos' && getToken()) fetchMyVideos();
    if (page === 'photos' && getToken()) fetchMyPhotos();
    if (page === 'apikeys' && getToken()) fetchApiKeys();
    if (page === 'plans') fetchPlans();
    if (page === 'subscription' && getToken()) fetchCurrentSub();
    if (page === 'publicchat') { if (getToken()) connectPublic(); else toast('Log in to use WebSockets', 'warning'); }
    if (page === 'privatechat') { if (getToken()) connectPrivate(); else toast('Log in to use WebSockets', 'warning'); }
    
    // Update status badges if we're already connected
    if (page === 'publicchat' && publicSocket && publicSocket.readyState === WebSocket.OPEN) {
        const el = $('publicStatus');
        if (el) { el.textContent = 'Connected'; el.className = 'badge badge-green'; }
    }
    if (page === 'privatechat' && privateSocket && privateSocket.readyState === WebSocket.OPEN) {
        const el = $('privateStatus');
        if (el) { el.textContent = 'Connected'; el.className = 'badge badge-green'; }
        const cStatus = $('privateConnectStatus');
        if (cStatus) { cStatus.textContent = 'Connected'; cStatus.className = 'whatsapp-chat-status'; }
    }
}

// ─── Auth ───
async function doLogin() {
    const u = $('loginUsername').value.trim();
    const p = $('loginPassword').value;
    if (!u || !p) return toast('Username and password required', 'error');
    try {
        const body = new URLSearchParams({ username: u, password: p });
        const r = await fetch('/api/v1/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body });
        if (!r.ok) throw new Error(`Login failed: ${r.status}`);
        const d = await r.json();
        ls(SK.jwt, d.token);
        if ($('jwtToken')) $('jwtToken').value = d.token;
        saveContext(d.userId, d.username);
        toast(`Welcome back, ${d.username}!`, 'success');
        onLoginSuccess(d);
    } catch (e) { toast(e.message, 'error'); }
}

async function doRegister() {
    const u = $('regUsername').value.trim();
    const e = $('regEmail').value.trim();
    const p = $('regPassword').value;
    const pc = $('regPasswordConfirm').value;
    if (!u || !e || !p || !pc) return toast('All fields are required', 'error');
    if (p !== pc) return toast('Passwords do not match', 'error');
    if (p.length < 8) return toast('Password must be at least 8 characters', 'error');
    try {
        const r = await fetch('/api/v1/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, email: e, password: p }) });
        if (!r.ok) { const err = await r.json(); throw new Error(err.message || `Registration failed: ${r.status}`); }
        toast('Account created! Please log in.', 'success');
        switchAuthTab('login');
        $('loginUsername').value = u;
    } catch (err) { toast(err.message, 'error'); }
}

function switchAuthTab(tab) {
    $('loginForm').style.display = tab === 'login' ? 'block' : 'none';
    $('registerForm').style.display = tab === 'register' ? 'block' : 'none';
    $('tabLogin').classList.toggle('active', tab === 'login');
    $('tabRegister').classList.toggle('active', tab === 'register');
}

async function doLogout() {
    try { await fetch('/api/v1/auth/logout', { method: 'POST', headers: authHeaders(true) }); } catch (_) {}
    [SK.jwt, SK.uid, SK.uname].forEach(lsRm);
    if ($('jwtToken')) $('jwtToken').value = '';
    toast('Logged out', 'info');
    clearDashboardData();
}

function onLoginSuccess(d) {
    loadProfile();
    connectPublic();
    connectPrivate();
    navigate('dashboard');
}

// ─── Profile / Dashboard ───
async function loadProfile() {
    try {
        const r = await fetch('/api/v1/users/me', { headers: { ...authHeaders(), 'Content-Type': 'application/json' } });
        if (!r.ok) throw new Error(`${r.status}`);
        const u = await r.json();
        saveContext(u.id, u.username);
        renderProfile(u);
        // Also load sub and plan
        fetchCurrentSub();
        fetchApiKeys();
    } catch (e) { toast('Failed to load profile: ' + e.message, 'error'); }
}

function renderProfile(u) {
    setText('profName', u.username);
    setText('profEmail', u.email);
    setText('profRole', u.role);
    setText('profId', u.id);
    // sidebar user display
    setText('sidebarUser', u.username);
    setText('sidebarRole', u.role);
}

function setText(id, val) { const el = $(id); if (el) el.textContent = val ?? '-'; }

function clearDashboardData() {
    ['profName','profEmail','profRole','profId','subStatus','subPlan','subStart','subEnd','planName','planPrice','planRate','planMedia'].forEach(id => setText(id, '-'));
    setText('sidebarUser', 'Not logged in');
    setText('sidebarRole', '');
}

// ─── Subscription ───
async function fetchCurrentSub() {
    try {
        const r = await fetch('/api/v1/sub/current', { headers: authHeaders() });
        if (!r.ok) { setText('subStatus', 'NONE'); setText('subStatusPage', 'NONE'); return; }
        const s = await r.json();
        setText('subStatus', s.status);
        setText('subPlan', s.planName);
        setText('subStart', s.startDate);
        setText('subEnd', s.endDate);
        setText('subStatusPage', s.status);
        setText('subPlanPage', s.planName);
        setText('subStartPage', s.startDate);
        setText('subEndPage', s.endDate);
        // Fetch matching plan details
        if (s.planName) {
            try {
                const pr = await fetch('/api/v1/plan');
                if (pr.ok) {
                    const plans = await pr.json();
                    const match = plans.find(p => p.name === s.planName);
                    if (match) {
                        setText('planName', match.name);
                        setText('planPrice', `$${match.monthlyPrice}/mo`);
                        setText('planRate', `${match.rateLimitPerMinute}/min`);
                        setText('planMedia', match.mediaCount);
                    }
                }
            } catch (_) {}
        }
    } catch (_) { setText('subStatus', 'NONE'); setText('subStatusPage', 'NONE'); }
}

async function subscribeToPlan() {
    const name = $('subPlanInput')?.value?.trim();
    if (!name) return toast('Enter a plan name', 'error');
    try {
        const r = await fetch(`/api/v1/sub?planName=${encodeURIComponent(name)}`, { method: 'POST', headers: authHeaders() });
        if (!r.ok) throw new Error(`${r.status}`);
        const s = await r.json();
        toast(`Subscribed to ${s.planName}!`, 'success');
        fetchCurrentSub();
    } catch (e) { toast('Subscribe failed: ' + e.message, 'error'); }
}

// ─── Plans ───
async function fetchPlans() {
    try {
        const r = await fetch('/api/v1/plan');
        if (!r.ok) throw new Error(`${r.status}`);
        const plans = await r.json();
        renderPlans(plans);
    } catch (e) { toast('Failed to load plans: ' + e.message, 'error'); }
}

function renderPlans(plans) {
    const c = $('plansList');
    if (!c) return;
    c.innerHTML = '';
    if (!plans.length) { c.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div>No plans available</div>'; return; }
    plans.forEach(p => {
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `<div class="card-header"><h3>${p.name}</h3><span class="badge ${p.isActive ? 'badge-green' : 'badge-red'}">${p.isActive ? 'Active' : 'Inactive'}</span></div>
            <div class="kv-grid">
                <div class="kv-item"><div class="kv-label">Price</div><div class="kv-value">$${p.monthlyPrice}/mo</div></div>
                <div class="kv-item"><div class="kv-label">Rate Limit</div><div class="kv-value">${p.rateLimitPerMinute}/min</div></div>
                <div class="kv-item"><div class="kv-label">Media Limit</div><div class="kv-value">${p.mediaCount}</div></div>
                <div class="kv-item"><div class="kv-label">Photo Size</div><div class="kv-value">${p.photoSize || '-'}</div></div>
            </div>`;
        c.appendChild(card);
    });
}

// ─── API Keys ───
async function fetchApiKeys() {
    try {
        const r = await fetch('/api/v1/apikey', { headers: authHeaders() });
        if (!r.ok) throw new Error(`${r.status}`);
        const keys = await r.json();
        renderApiKeys(keys);
    } catch (_) {}
}

function renderApiKeys(keys) {
    const c = $('apiKeyList');
    if (!c) return;
    c.innerHTML = '';
    if (!keys.length) { c.innerHTML = '<div class="empty-state"><div class="empty-icon">🔑</div>No API keys yet</div>'; return; }
    keys.forEach(k => {
        const item = document.createElement('div');
        item.className = 'list-item';
        item.innerHTML = `<div class="list-item-info"><div class="list-item-title">${k.name} <span class="badge ${k.revoked ? 'badge-red' : 'badge-green'}">${k.revoked ? 'Revoked' : 'Active'}</span></div>
            <div class="list-item-sub">Prefix: ${k.prefix} · Created: ${k.createdAt ? new Date(k.createdAt).toLocaleDateString() : '-'}</div></div>
            <div class="list-item-actions"><button class="btn-danger" onclick="revokeApiKey(${k.id})">Revoke</button></div>`;
        c.appendChild(item);
    });
}

async function createApiKey() {
    const name = $('newKeyName')?.value?.trim();
    if (!name) return toast('Enter a key name', 'error');
    try {
        const r = await fetch(`/api/v1/apikey/${encodeURIComponent(name)}`, { method: 'POST', headers: authHeaders() });
        if (!r.ok) throw new Error(`${r.status}`);
        const k = await r.json();
        if (k.rawKey) toast(`Key created! Raw key: ${k.rawKey}`, 'success');
        else toast('API key created!', 'success');
        $('newKeyName').value = '';
        fetchApiKeys();
    } catch (e) { toast('Failed: ' + e.message, 'error'); }
}

async function revokeApiKey(id) {
    try {
        await fetch(`/api/v1/apikey/${id}`, { method: 'DELETE', headers: authHeaders() });
        toast('Key revoked', 'success');
        fetchApiKeys();
    } catch (e) { toast('Failed: ' + e.message, 'error'); }
}

// ─── Videos ───
function vlog(msg) {
    const l = $('videoLog');
    if (!l) return;
    const d = document.createElement('div');
    d.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
    l.prepend(d);
}

async function fetchMyVideos() {
    try {
        const r = await fetch(withApiKey('/api/v1/video'), { headers: authHeaders(true) });
        if (!r.ok) throw new Error(`${r.status}`);
        const vids = await r.json();
        renderVideoList(vids);
    } catch (e) { vlog('Failed to load videos: ' + e.message); }
}

function renderVideoList(videos) {
    const c = $('videoList');
    if (!c) return;
    c.innerHTML = '';
    if (!videos?.length) { c.innerHTML = '<div class="empty-state"><div class="empty-icon">🎬</div>No videos uploaded yet</div>'; return; }
    videos.forEach(v => {
        const stage = (v.stage || 'PROCESSING').toUpperCase();
        const canPlay = stage === 'COMPLETED';
        const item = document.createElement('div');
        item.className = 'list-item';
        item.innerHTML = `<div class="list-item-info"><div class="list-item-title">${v.mediaTitle || '(untitled)'} <span class="badge ${canPlay ? 'badge-green' : 'badge-yellow'}">${stage}</span></div>
            <div class="list-item-sub">${v.mediaId}</div></div>
            <div class="list-item-actions">
                <button ${canPlay ? '' : 'disabled'} onclick="playVideo('${v.mediaId}')">▶ Play</button>
                <button class="btn-danger" onclick="deleteVideo('${v.mediaId}')">Delete</button>
            </div>`;
        c.appendChild(item);
    });
}

async function uploadVideo() {
    const f = $('videoFileInput')?.files?.[0];
    if (!f) return toast('Choose a video file first', 'error');
    const fd = new FormData(); fd.append('video', f);
    try {
        vlog('Uploading ' + f.name + '...');
        const r = await fetch(withApiKey('/api/v1/video'), { method: 'POST', headers: authHeaders(true), body: fd });
        if (!r.ok) throw new Error(`${r.status}`);
        const d = await r.json();
        toast('Video uploaded!', 'success');
        vlog(`Upload success: ${d.mediaId} (${d.stage})`);
        fetchMyVideos();
        if (d.mediaId) pollVideo(d.mediaId);
    } catch (e) { toast('Upload failed: ' + e.message, 'error'); }
}

async function pollVideo(id, max = 30) {
    for (let i = 0; i < max; i++) {
        try {
            const r = await fetch(withApiKey(`/api/v1/video/${encodeURIComponent(id)}/status`), { headers: authHeaders(true) });
            if (r.ok) { const s = await r.json(); if (s.stage === 'COMPLETED') { vlog(`${id} COMPLETED`); fetchMyVideos(); return; } vlog(`${id}: ${s.stage}`); }
        } catch (_) {}
        await new Promise(r => setTimeout(r, 4000));
    }
}

async function deleteVideo(id) {
    try {
        await fetch(withApiKey(`/api/v1/video/${encodeURIComponent(id)}`), { method: 'DELETE', headers: authHeaders(true) });
        toast('Video deleted', 'success');
        fetchMyVideos();
    } catch (e) { toast('Delete failed: ' + e.message, 'error'); }
}

async function playVideo(id) {
    $('videoIdInput').value = id;
    activeQuality = 'source';
    await switchQuality('source');
    fetchQualities(id);
}

function buildStreamUrl(quality) {
    const id = $('videoIdInput')?.value?.trim();
    if (!id) throw new Error('No video ID');
    const u = new URL(`/api/v1/video/${encodeURIComponent(id)}`, location.origin);
    const k = getApiKey(); if (k) u.searchParams.set('apikey', k);
    if (quality && quality !== 'source') u.searchParams.set('quality', quality);
    return u.toString();
}

async function fetchQualities(id) {
    try {
        const r = await fetch(withApiKey(`/api/v1/video/${encodeURIComponent(id)}/qualities`), { headers: authHeaders(true) });
        if (!r.ok) throw new Error(`${r.status}`);
        const q = await r.json();
        renderQualityButtons(['source', ...q]);
    } catch (_) { renderQualityButtons(['source']); }
}

function renderQualityButtons(quals) {
    const c = $('qualities');
    if (!c) return;
    c.innerHTML = '';
    quals.forEach(q => {
        const b = document.createElement('button');
        b.textContent = q; b.dataset.quality = q;
        if (q === activeQuality) b.classList.add('active');
        b.onclick = () => switchQuality(q);
        c.appendChild(b);
    });
}

async function switchQuality(q) {
    const player = $('player');
    if (!player) return;
    const vid = $('videoIdInput')?.value?.trim();
    if (!vid) return;
    switchVersion++;
    const ver = switchVersion;
    if (switchController) switchController.abort();
    switchController = new AbortController();
    const resume = player.currentTime || 0;
    const wasPlaying = !player.paused;
    if (currentVideoId !== vid) { player.pause(); player.removeAttribute('src'); }
    activeQuality = q;
    document.querySelectorAll('#qualities button').forEach(b => b.classList.toggle('active', b.dataset.quality === q));
    if (currentObjectUrl) { URL.revokeObjectURL(currentObjectUrl); currentObjectUrl = null; }
    const url = buildStreamUrl(q);
    // Try direct stream first
    try {
        const probe = await fetch(url, { method: 'GET', headers: { Range: 'bytes=0-0' }, credentials: 'include', cache: 'no-store' });
        if (probe.ok || probe.status === 206) { player.src = url; vlog(`${q} (direct)`); }
        else throw new Error('no direct');
    } catch (_) {
        try {
            const r = await fetch(url, { headers: authHeaders(true), signal: switchController.signal });
            if (!r.ok) throw new Error(`${r.status}`);
            const blob = await r.blob();
            if (ver !== switchVersion) return;
            currentObjectUrl = URL.createObjectURL(blob);
            player.src = currentObjectUrl;
            vlog(`${q} (blob)`);
        } catch (e) { if (e.name !== 'AbortError') vlog('Stream failed: ' + e.message); return; }
    }
    currentVideoId = vid;
    player.load();
    player.addEventListener('loadedmetadata', function h() {
        player.removeEventListener('loadedmetadata', h);
        if (Number.isFinite(resume)) player.currentTime = Math.min(resume, Math.max((player.duration || resume) - 0.1, 0));
        if (wasPlaying) player.play().catch(() => {});
    });
}

// ─── Photos ───
function plog(msg) {
    const l = $('photoLog');
    if (!l) return;
    const d = document.createElement('div');
    d.textContent = `[${new Date().toLocaleTimeString()}] ${msg}`;
    l.prepend(d);
}

async function fetchMyPhotos() {
    try {
        const r = await fetch(withApiKey('/api/v1/photo'), { headers: authHeaders(true) });
        if (!r.ok) throw new Error(`${r.status}`);
        renderPhotoList(await r.json());
    } catch (e) { plog('Failed: ' + e.message); }
}

function renderPhotoList(photos) {
    const c = $('photoList');
    if (!c) return;
    c.innerHTML = '';
    if (!photos?.length) { c.innerHTML = '<div class="empty-state"><div class="empty-icon">📷</div>No photos uploaded yet</div>'; return; }
    photos.forEach(p => {
        const item = document.createElement('div');
        item.className = 'list-item';
        item.innerHTML = `<div class="list-item-info"><div class="list-item-title">${p.mediaTitle || '(untitled)'}</div><div class="list-item-sub">${p.mediaId}</div></div>
            <div class="list-item-actions">
                <button onclick="previewPhoto('${p.mediaId}')">👁 View</button>
                <button class="btn-danger" onclick="deletePhoto('${p.mediaId}')">Delete</button>
            </div>`;
        c.appendChild(item);
    });
}

async function uploadPhoto() {
    const f = $('photoFileInput')?.files?.[0];
    if (!f) return toast('Choose a photo first', 'error');
    const fd = new FormData(); fd.append('photo', f);
    try {
        const r = await fetch(withApiKey('/api/v1/photo'), { method: 'POST', headers: authHeaders(true), body: fd });
        if (!r.ok) throw new Error(`${r.status}`);
        const d = await r.json();
        toast('Photo uploaded!', 'success');
        fetchMyPhotos();
        if (d.mediaId) previewPhoto(d.mediaId);
    } catch (e) { toast('Upload failed: ' + e.message, 'error'); }
}

async function previewPhoto(id) {
    try {
        const r = await fetch(withApiKey(`/api/v1/photo/${encodeURIComponent(id)}`), { headers: authHeaders(true) });
        if (!r.ok) throw new Error(`${r.status}`);
        const blob = await r.blob();
        if (currentPhotoPreviewUrl) URL.revokeObjectURL(currentPhotoPreviewUrl);
        currentPhotoPreviewUrl = URL.createObjectURL(blob);
        const img = $('photoPreview');
        img.src = currentPhotoPreviewUrl;
        img.style.display = 'block';
        plog('Previewing ' + id);
    } catch (e) { plog('Preview failed: ' + e.message); }
}

async function deletePhoto(id) {
    try {
        await fetch(withApiKey(`/api/v1/photo/${encodeURIComponent(id)}`), { method: 'DELETE', headers: authHeaders(true) });
        toast('Photo deleted', 'success');
        fetchMyPhotos();
    } catch (e) { toast('Delete failed: ' + e.message, 'error'); }
}

// ─── GraphQL ───
const GQL_GET_USER = `query GetUser($id: ID!) { getUser(id: $id) { id username email role subResponse { id planName userId planId startDate endDate status } planResponse { id name monthlyPrice rateLimitPerMinute isActive mediaCount } apiKeyResponse { id prefix name createdAt expiresAt lastUsed revoked } videoResponse { mediaId mediaTitle type stage isDeleted } photoResponse { mediaId mediaTitle type stage isDeleted } } }`;

async function runGraphQL() {
    const id = $('gqlUserId')?.value?.trim() || ls(SK.uid) || '';
    if (!id) return toast('Enter a User ID', 'error');
    try {
        const r = await fetch('/graphql', { method: 'POST', headers: { ...authHeaders(), 'Content-Type': 'application/json' }, body: JSON.stringify({ query: GQL_GET_USER, variables: { id } }) });
        const json = await r.json();
        if (json.errors) throw new Error(json.errors[0].message);
        $('gqlResult').textContent = JSON.stringify(json.data, null, 2);
        // also render dashboard data from graphql
        const u = json.data?.getUser;
        if (u) {
            renderProfile(u);
            if (u.subResponse) { setText('subStatus', u.subResponse.status); setText('subPlan', u.planResponse?.name || u.subResponse.planName); setText('subStart', u.subResponse.startDate); setText('subEnd', u.subResponse.endDate); }
            if (u.planResponse) { setText('planName', u.planResponse.name); setText('planPrice', `$${u.planResponse.monthlyPrice}`); setText('planRate', `${u.planResponse.rateLimitPerMinute}/min`); setText('planMedia', u.planResponse.mediaCount); }
            if (u.apiKeyResponse) renderApiKeys(u.apiKeyResponse);
        }
        toast('GraphQL query successful', 'success');
    } catch (e) { toast('GraphQL error: ' + e.message, 'error'); $('gqlResult').textContent = e.message; }
}

// ─── WebSockets ───
function connectPublic() {
    const token = getToken();
    if (!token) return;

    if (publicSocket && (publicSocket.readyState === WebSocket.OPEN || publicSocket.readyState === WebSocket.CONNECTING)) {
        return;
    }

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = location.host;

    publicSocket = new WebSocket(`${protocol}//${host}/ws/public?token=${token}`);
    
    publicSocket.onopen = () => {
        if (publicReconnectTimer) { clearInterval(publicReconnectTimer); publicReconnectTimer = null; }
        const el = $('publicStatus');
        if (el) { el.textContent = 'Connected'; el.className = 'badge badge-green'; }
        addMessage('publicMessages', 'System', 'Connected to global broadcast', 'system');
    };

    publicSocket.onmessage = (e) => {
        addMessage('publicMessages', 'Broadcast', e.data);
    };

    publicSocket.onclose = () => {
        const el = $('publicStatus');
        if (el) { el.textContent = 'Disconnected'; el.className = 'badge'; }
        if (!publicReconnectTimer && currentPage === 'publicchat') {
            publicReconnectTimer = setInterval(connectPublic, 5000);
        }
    };

    publicSocket.onerror = () => {
        const el = $('publicStatus');
        if (el) { el.textContent = 'Connection Error'; el.className = 'badge badge-red'; }
    };
}

function connectPrivate() {
    const token = getToken();
    if (!token) return;

    if (privateSocket && (privateSocket.readyState === WebSocket.OPEN || privateSocket.readyState === WebSocket.CONNECTING)) {
        return;
    }

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = location.host;

    privateSocket = new WebSocket(`${protocol}//${host}/ws/private?token=${token}`);
    
    privateSocket.onopen = () => {
        if (privateReconnectTimer) { clearInterval(privateReconnectTimer); privateReconnectTimer = null; }
        const el = $('privateStatus');
        if (el) { el.textContent = 'Connected'; el.className = 'badge badge-green'; }
        const cStatus = $('privateConnectStatus');
        if (cStatus) { cStatus.textContent = 'Connected'; cStatus.className = 'whatsapp-chat-status'; }
        addMessage('privateMessages', 'System', 'Secure connection established', 'system');
    };

    privateSocket.onmessage = (e) => {
        try {
            console.log('WS Private Message:', e.data);
            const data = JSON.parse(e.data);
            
            if (data.type === 'ERROR') {
                toast(data.message || 'Server error', 'error');
                return;
            }

            const from = data.from || 'Unknown';
            const content = data.content || '';
            const mediaUrl = data.mediaUrl;
            const type = data.type || 'TEXT';
            
            // Save to history
            saveToHistory(from, from, content, 'remote', mediaUrl, type);
            
            if (activeContact && activeContact.toLowerCase() === from.toLowerCase()) {
                addMessage('privateMessages', from, content, 'remote', mediaUrl, type);
            } else {
                toast(`New message from ${from}`, 'info');
            }
            updateContactList();
        } catch (err) {
            console.error('WS Private error', err);
        }
    };

    privateSocket.onclose = () => {
        const el = $('privateStatus');
        if (el) { el.textContent = 'Disconnected'; el.className = 'badge'; }
        const cStatus = $('privateConnectStatus');
        if (cStatus) { cStatus.textContent = 'Offline'; cStatus.className = 'whatsapp-chat-status offline'; }
        
        if (!privateReconnectTimer && currentPage === 'privatechat') {
            privateReconnectTimer = setInterval(connectPrivate, 5000);
        }
    };

    privateSocket.onerror = () => {
        const el = $('privateStatus');
        if (el) { el.textContent = 'Connection Error'; el.className = 'badge badge-red'; }
    };
}

function disconnectPublic() {
    if (publicSocket) publicSocket.close();
    if (publicReconnectTimer) { clearInterval(publicReconnectTimer); publicReconnectTimer = null; }
}

function disconnectPrivate() {
    if (privateSocket) privateSocket.close();
    if (privateReconnectTimer) { clearInterval(privateReconnectTimer); privateReconnectTimer = null; }
}

function sendPublicMessage() {
    const input = $('publicInput');
    const msg = input.value.trim();
    if (!msg || !publicSocket || publicSocket.readyState !== WebSocket.OPEN) return;
    publicSocket.send(msg);
    addMessage('publicMessages', 'You', msg, 'self');
    input.value = '';
}

function sendPrivateMessage(mediaData = null) {
    const target = activeContact;
    const input = $('privateInput');
    const msg = input.value.trim();
    
    if (!target) return toast('Select a contact first', 'error');
    if (!privateSocket || privateSocket.readyState !== WebSocket.OPEN) return toast('Not connected to chat', 'error');

    let payload = {
        to: target,
        from: ls(SK.uid) || '0',
        timestamp: Date.now()
    };

    if (mediaData) {
        payload.type = mediaData.type;
        payload.mediaUrl = mediaData.url;
        payload.content = mediaData.caption || '';
    } else {
        if (!msg) return;
        payload.type = 'TEXT';
        payload.content = msg;
    }

    privateSocket.send(JSON.stringify(payload));
    saveToHistory(target, 'You', payload.content, 'self', payload.mediaUrl, payload.type);
    addMessage('privateMessages', 'You', payload.content, 'self', payload.mediaUrl, payload.type);
    if (!mediaData) input.value = '';
}

async function handleChatMedia(input) {
    const file = input.files[0];
    if (!file) return;

    const isImage = file.type.startsWith('image/');
    const isVideo = file.type.startsWith('video/');
    if (!isImage && !isVideo) return toast('Unsupported file type', 'error');

    const type = isImage ? 'IMAGE' : 'VIDEO';
    toast(`Uploading ${type.toLowerCase()}...`, 'info');

    const fd = new FormData();
    fd.append(isImage ? 'photo' : 'video', file);

    try {
        const endpoint = isImage ? '/api/v1/photo' : '/api/v1/video';
        const r = await fetch(withApiKey(endpoint), { method: 'POST', headers: authHeaders(true), body: fd });
        if (!r.ok) throw new Error(`${r.status}`);
        const d = await r.json();
        
        const mediaId = d.mediaId;
        const mediaUrl = isImage ? `/api/v1/photo/${mediaId}` : `/api/v1/video/${mediaId}`;
        
        sendPrivateMessage({
            type: type,
            url: mediaUrl,
            caption: file.name
        });
        
        toast(`${type} sent!`, 'success');
    } catch (e) {
        toast('Media upload failed: ' + e.message, 'error');
    } finally {
        input.value = '';
    }
}

// ─── WhatsApp Logic ───
function addNewContact() {
    const nameOrId = $('contactSearch').value.trim();
    if (!nameOrId) return;
    if (nameOrId === ls(SK.uname) || nameOrId === ls(SK.uid)) return toast("You can't chat with yourself", "warning");
    
    if (!contactList.includes(nameOrId)) {
        contactList.unshift(nameOrId);
        ls('a0.contactList', JSON.stringify(contactList));
    }
    $('contactSearch').value = '';
    switchContact(nameOrId);
}

function updateContactList() {
    const container = $('contactList');
    if (!container) return;
    container.innerHTML = '';
    
    contactList.forEach(name => {
        const history = chatHistory[name] || [];
        const last = history[history.length - 1];
        let lastMsg = 'No messages';
        if (last) {
            if (last.mediaType === 'IMAGE') lastMsg = '📷 Photo';
            else if (last.mediaType === 'VIDEO') lastMsg = '🎥 Video';
            else lastMsg = last.text || '...';
        }
        
        const el = document.createElement('div');
        el.className = `whatsapp-contact ${activeContact === name ? 'active' : ''}`;
        el.onclick = () => switchContact(name);
        el.innerHTML = `
            <div class="whatsapp-avatar">${(name[0] || '?').toUpperCase()}</div>
            <div class="whatsapp-contact-info">
                <div class="whatsapp-contact-name">${name}</div>
                <div class="whatsapp-contact-last">${lastMsg}</div>
            </div>
        `;
        container.appendChild(el);
    });
}

function switchContact(name) {
    activeContact = name;
    $('chatPlaceholder').style.display = 'none';
    $('activeChat').style.display = 'flex';
    
    $('activeChatName').textContent = name;
    $('activeChatAvatar').textContent = (name[0] || '?').toUpperCase();
    
    const msgContainer = $('privateMessages');
    msgContainer.innerHTML = '';
    
    const history = chatHistory[name] || [];
    history.forEach(m => {
        addMessage('privateMessages', m.sender, m.text, m.type, m.mediaUrl, m.mediaType);
    });
    
    updateContactList();
}

function saveToHistory(contact, sender, text, type, mediaUrl = null, mediaType = 'TEXT') {
    if (!contactList.some(c => c.toLowerCase() === contact.toLowerCase())) {
        contactList.unshift(contact);
        ls('a0.contactList', JSON.stringify(contactList));
    }
    if (!chatHistory[contact]) chatHistory[contact] = [];
    chatHistory[contact].push({ sender, text, type, mediaUrl, mediaType, time: Date.now() });
    ls('a0.chatHistory', JSON.stringify(chatHistory));
}

function addMessage(containerId, sender, text, type = '', mediaUrl = null, mediaType = 'TEXT') {
    const c = $(containerId);
    if (!c) return;

    // Remove empty state
    const empty = c.querySelector('.empty-state');
    if (empty) empty.remove();

    const m = document.createElement('div');
    m.className = type === 'system' ? 'message-system' : (type === 'self' ? 'message-self' : 'message-remote');

    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    
    let mediaHtml = '';
    if (mediaUrl) {
        const fullMediaUrl = withApiKey(mediaUrl);
        if (mediaType === 'IMAGE') {
            mediaHtml = `<div class="message-media-container"><img src="${fullMediaUrl}" loading="lazy" onclick="window.open('${fullMediaUrl}')" /></div>`;
        } else if (mediaType === 'VIDEO') {
            mediaHtml = `<div class="message-media-container"><video src="${fullMediaUrl}" controls preload="metadata"></video></div>`;
        }
    }

    // Truncate long filenames/text if it's a media caption
    let displayText = text || '';
    if (mediaUrl && displayText.length > 60) {
        displayText = displayText.substring(0, 57) + '...';
    }

    m.innerHTML = `
        <div class="message-header">
            <span class="message-sender">${sender}</span>
            <span class="message-time">${time}</span>
        </div>
        ${mediaHtml}
        <div class="message-text">${displayText}</div>
    `;
    
    c.appendChild(m);
    c.scrollTop = c.scrollHeight;
}

// ─── Init ───
function init() {
    // Mobile sidebar
    $('burgerBtn')?.addEventListener('click', () => { document.querySelector('.sidebar').classList.toggle('open'); document.querySelector('.sidebar-overlay').classList.toggle('open'); });
    document.querySelector('.sidebar-overlay')?.addEventListener('click', () => { document.querySelector('.sidebar').classList.remove('open'); document.querySelector('.sidebar-overlay').classList.remove('open'); });

    // Restore saved state
    const savedToken = ls(SK.jwt);
    const savedUid = ls(SK.uid);
    const savedApiKey = ls(SK.apikey);
    if (savedToken && $('jwtToken')) $('jwtToken').value = savedToken;
    if (savedUid && $('userId')) $('userId').value = savedUid;
    if (savedApiKey && $('apiKey')) $('apiKey').value = savedApiKey;
    if (savedUid && $('gqlUserId')) $('gqlUserId').value = savedUid;

    if (savedToken) {
        loadProfile();
        connectPublic();
        connectPrivate();
    }

    updateContactList();
    navigate('dashboard');

    // Video player events
    const player = $('player');
    if (player) {
        ['waiting','playing','stalled','error'].forEach(e => player.addEventListener(e, () => vlog(`Event: ${e} @ ${player.currentTime?.toFixed(2)}s`)));
    }

    window.addEventListener('beforeunload', () => {
        if (currentObjectUrl) URL.revokeObjectURL(currentObjectUrl);
        if (currentPhotoPreviewUrl) URL.revokeObjectURL(currentPhotoPreviewUrl);
    });
}

document.addEventListener('DOMContentLoaded', init);

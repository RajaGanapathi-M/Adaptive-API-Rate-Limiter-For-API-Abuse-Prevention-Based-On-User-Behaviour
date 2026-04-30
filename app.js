'use strict';

const state = {
  session: {
    requests: 0,
    success: 0,
    blocked: 0,
    lastStatus: 'Idle',
    history: [],
  },
  generatedKey: '',
  statsHistory: [],
};

const elements = {
  baseUrl: document.getElementById('baseUrl'),
  clientName: document.getElementById('clientName'),
  apiKeyInput: document.getElementById('apiKeyInput'),
  statsClientId: document.getElementById('statsClientId'),
  serverBadge: document.getElementById('serverBadge'),
  serverBadgeText: document.getElementById('serverBadgeText'),
  summaryRequests: document.getElementById('summaryRequests'),
  summarySuccess: document.getElementById('summarySuccess'),
  summaryBlocked: document.getElementById('summaryBlocked'),
  summaryLastStatus: document.getElementById('summaryLastStatus'),
  endpointPreset: document.getElementById('endpointPreset'),
  customMethodField: document.getElementById('customMethodField'),
  customPathField: document.getElementById('customPathField'),
  customMethod: document.getElementById('customMethod'),
  customPath: document.getElementById('customPath'),
  burstCount: document.getElementById('burstCount'),
  requestMeta: document.getElementById('requestMeta'),
  activityLog: document.getElementById('activityLog'),
  responsePreview: document.getElementById('responsePreview'),
  payloadStatus: document.getElementById('payloadStatus'),
  generatedKeyValue: document.getElementById('generatedKeyValue'),
  generatedKeyMessage: document.getElementById('generatedKeyMessage'),
  revokeKeyInput: document.getElementById('revokeKeyInput'),
  revokeMessage: document.getElementById('revokeMessage'),
  pageTitle: document.getElementById('pageTitle'),
  sidebar: document.getElementById('sidebar'),
  menuBtn: document.getElementById('menuBtn'),
};

Chart.defaults.font.family = "'Manrope', sans-serif";
Chart.defaults.color = '#6b7280';

elements.baseUrl.value = getStoredBaseUrl();
elements.clientName.value = 'inventory-service';
toggleCustomFields();

const sessionChart = new Chart(document.getElementById('sessionChart'), {
  type: 'line',
  data: {
    labels: Array.from({ length: 12 }, (_, index) => `${index + 1}`),
    datasets: [
      {
        label: 'HTTP 2xx',
        data: Array(12).fill(0),
        borderColor: '#198754',
        backgroundColor: 'rgba(25, 135, 84, 0.12)',
        fill: true,
        tension: 0.35,
        pointRadius: 0,
      },
      {
        label: 'HTTP 429 / Error',
        data: Array(12).fill(0),
        borderColor: '#c2410c',
        backgroundColor: 'rgba(194, 65, 12, 0.08)',
        fill: true,
        tension: 0.35,
        pointRadius: 0,
      },
    ],
  },
  options: {
    plugins: {
      legend: {
        position: 'bottom',
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
      },
      y: {
        beginAtZero: true,
        ticks: {
          precision: 0,
        },
      },
    },
  },
});

const tokensChart = new Chart(document.getElementById('tokensChart'), {
  type: 'line',
  data: {
    labels: [],
    datasets: [
      {
        label: 'Live Tokens',
        data: [],
        borderColor: '#c96f39',
        backgroundColor: 'rgba(201, 111, 57, 0.12)',
        fill: true,
        tension: 0.28,
      },
      {
        label: 'Rejected Requests',
        data: [],
        borderColor: '#1f2937',
        backgroundColor: 'rgba(31, 41, 55, 0.05)',
        fill: false,
        tension: 0.28,
      },
    ],
  },
  options: {
    plugins: {
      legend: {
        position: 'bottom',
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
      },
      y: {
        beginAtZero: true,
      },
    },
  },
});

document.querySelectorAll('.nav-link').forEach((link) => {
  link.addEventListener('click', (event) => {
    event.preventDefault();
    const sectionId = link.dataset.section;
    setActiveSection(sectionId, link);
  });
});

elements.menuBtn.addEventListener('click', () => {
  elements.sidebar.classList.toggle('open');
});

elements.endpointPreset.addEventListener('change', toggleCustomFields);
document.getElementById('connectBtn').addEventListener('click', connectBackend);
document.getElementById('refreshOverviewBtn').addEventListener('click', connectBackend);
document.getElementById('fetchStatsBtn').addEventListener('click', fetchStats);
document.getElementById('generateKeyBtn').addEventListener('click', generateKey);
document.getElementById('revokeKeyBtn').addEventListener('click', revokeKey);
document.getElementById('useGeneratedKeyBtn').addEventListener('click', useLastGeneratedKey);
document.getElementById('sendRequestBtn').addEventListener('click', sendRequest);
document.getElementById('sendBurstBtn').addEventListener('click', sendBurst);
document.getElementById('clearLogBtn').addEventListener('click', clearLog);

connectBackend();

function getStoredBaseUrl() {
  const stored = window.localStorage.getItem('rate-limiter-base-url');
  if (stored) {
    return stored;
  }

  if (window.location.protocol.startsWith('http')) {
    return window.location.origin;
  }

  return 'http://localhost:8080';
}

function normalizeBaseUrl() {
  const raw = elements.baseUrl.value.trim();
  return raw.endsWith('/') ? raw.slice(0, -1) : raw;
}

function setActiveSection(sectionId, link) {
  document.querySelectorAll('.nav-link').forEach((item) => item.classList.remove('active'));
  document.querySelectorAll('.section-block').forEach((section) => section.classList.remove('active'));

  link.classList.add('active');
  document.getElementById(sectionId).classList.add('active');
  elements.pageTitle.textContent = link.textContent.trim();
  elements.sidebar.classList.remove('open');
}

function toggleCustomFields() {
  const isCustom = elements.endpointPreset.value === 'custom';
  elements.customMethodField.style.display = isCustom ? 'flex' : 'none';
  elements.customPathField.style.display = isCustom ? 'flex' : 'none';
}

function getHeaders() {
  const headers = {
    Accept: 'application/json, text/plain, */*',
  };

  const apiKey = elements.apiKeyInput.value.trim();
  if (apiKey) {
    headers['X-API-Key'] = apiKey;
  }

  return headers;
}

async function connectBackend() {
  const baseUrl = normalizeBaseUrl();
  if (!baseUrl) {
    setServerStatus('offline', 'Enter a backend URL');
    return;
  }

  window.localStorage.setItem('rate-limiter-base-url', baseUrl);
  elements.requestMeta.textContent = `Connected base URL: ${baseUrl}`;

  try {
    const result = await request(`${baseUrl}/api/hello`, {
      method: 'GET',
      headers: getHeaders(),
    });

    setServerStatus('online', 'Backend reachable');
    updateLatestResponse(result);
    addLog('ok', `Connected successfully to ${baseUrl}`);
  } catch (error) {
    setServerStatus('offline', error.message || 'Backend unavailable');
    addLog('error', `Connection failed: ${error.message}`);
  }
}

function setServerStatus(type, text) {
  elements.serverBadge.classList.remove('online', 'offline');
  elements.serverBadge.classList.add(type);
  elements.serverBadgeText.textContent = text;
}

function buildRequestConfig() {
  const preset = elements.endpointPreset.value;
  const baseUrl = normalizeBaseUrl();
  const headers = getHeaders();

  if (!baseUrl) {
    throw new Error('Backend base URL is required.');
  }

  if (preset === 'hello') {
    return {
      method: 'GET',
      url: `${baseUrl}/api/hello`,
      description: 'Testing the public hello endpoint.',
    };
  }

  if (preset === 'stats') {
    const clientId = elements.statsClientId.value.trim();
    if (!clientId) {
      throw new Error('Client ID is required for /admin/stats.');
    }

    return {
      method: 'GET',
      url: `${baseUrl}/admin/stats?clientId=${encodeURIComponent(clientId)}`,
      description: `Fetching stats for client ID: ${clientId}`,
    };
  }

  if (preset === 'generate') {
    const clientName = elements.clientName.value.trim();
    if (!clientName) {
      throw new Error('Client name is required to generate a key.');
    }

    return {
      method: 'POST',
      url: `${baseUrl}/admin/generate-key?clientName=${encodeURIComponent(clientName)}`,
      description: `Generating an API key for ${clientName}`,
    };
  }

  if (preset === 'revoke') {
    const apiKey = elements.revokeKeyInput.value.trim() || elements.apiKeyInput.value.trim();
    if (!apiKey) {
      throw new Error('Provide an API key to revoke.');
    }

    return {
      method: 'DELETE',
      url: `${baseUrl}/admin/revoke-key?apiKey=${encodeURIComponent(apiKey)}`,
      description: `Revoking API key ${shorten(apiKey)}`,
    };
  }

  const customPath = elements.customPath.value.trim();
  if (!customPath) {
    throw new Error('Custom path is required.');
  }

  return {
    method: elements.customMethod.value,
    url: `${baseUrl}${customPath.startsWith('/') ? customPath : `/${customPath}`}`,
    description: `Sending a custom ${elements.customMethod.value} request.`,
    headers,
  };
}

async function sendRequest() {
  try {
    const config = buildRequestConfig();
    elements.requestMeta.textContent = config.description;

    const result = await request(config.url, {
      method: config.method,
      headers: config.headers || getHeaders(),
    });

    updateSession(result.status);
    updateLatestResponse(result);
    addLog('ok', `${config.method} ${config.url} -> ${result.status}`);

    if (elements.endpointPreset.value === 'generate' && result.data && result.data.apiKey) {
      applyGeneratedKey(result.data);
    }

    if (elements.endpointPreset.value === 'stats') {
      populateStats(result.data);
    }

    if (elements.endpointPreset.value === 'revoke' && result.data && result.data.message) {
      elements.revokeMessage.textContent = result.data.message;
    }
  } catch (error) {
    updateSession(error.status || 0);
    updateLatestResponse({
      status: error.status || 'NETWORK',
      data: error.data || { message: error.message },
      elapsedMs: error.elapsedMs || 0,
    });
    addLog('error', error.message);
  }
}

async function sendBurst() {
  const total = Math.min(Math.max(Number(elements.burstCount.value) || 1, 1), 25);
  addLog('info', `Starting burst of ${total} requests.`);

  for (let index = 0; index < total; index += 1) {
    await sendRequest();
    await pause(160);
  }

  addLog('info', `Burst finished with ${total} requests.`);
}

async function fetchStats() {
  const clientId = elements.statsClientId.value.trim();
  if (!clientId) {
    addLog('error', 'Enter a client ID before fetching stats.');
    return;
  }

  try {
    const result = await request(
      `${normalizeBaseUrl()}/admin/stats?clientId=${encodeURIComponent(clientId)}`,
      {
        method: 'GET',
        headers: getHeaders(),
      }
    );

    updateLatestResponse(result);
    addLog('ok', `Fetched stats for ${shorten(clientId)}`);
    populateStats(result.data);
  } catch (error) {
    updateLatestResponse({
      status: error.status || 'NETWORK',
      data: error.data || { message: error.message },
      elapsedMs: error.elapsedMs || 0,
    });
    addLog('error', `Stats request failed: ${error.message}`);
  }
}

async function generateKey() {
  const clientName = elements.clientName.value.trim();
  if (!clientName) {
    addLog('error', 'Client name is required to generate an API key.');
    return;
  }

  try {
    const result = await request(
      `${normalizeBaseUrl()}/admin/generate-key?clientName=${encodeURIComponent(clientName)}`,
      {
        method: 'POST',
        headers: getHeaders(),
      }
    );

    updateLatestResponse(result);
    addLog('ok', `Generated a key for ${clientName}`);
    applyGeneratedKey(result.data);
  } catch (error) {
    updateLatestResponse({
      status: error.status || 'NETWORK',
      data: error.data || { message: error.message },
      elapsedMs: error.elapsedMs || 0,
    });
    addLog('error', `Key generation failed: ${error.message}`);
  }
}

async function revokeKey() {
  const apiKey = elements.revokeKeyInput.value.trim() || elements.apiKeyInput.value.trim();
  if (!apiKey) {
    addLog('error', 'Provide an API key to revoke.');
    return;
  }

  try {
    const result = await request(
      `${normalizeBaseUrl()}/admin/revoke-key?apiKey=${encodeURIComponent(apiKey)}`,
      {
        method: 'DELETE',
        headers: getHeaders(),
      }
    );

    updateLatestResponse(result);
    elements.revokeMessage.textContent = result.data?.message || 'Key revoked.';
    addLog('ok', `Revoked key ${shorten(apiKey)}`);
  } catch (error) {
    updateLatestResponse({
      status: error.status || 'NETWORK',
      data: error.data || { message: error.message },
      elapsedMs: error.elapsedMs || 0,
    });
    addLog('error', `Key revoke failed: ${error.message}`);
  }
}

function useLastGeneratedKey() {
  if (!state.generatedKey) {
    addLog('info', 'Generate a key first, then reuse it here.');
    return;
  }

  elements.apiKeyInput.value = state.generatedKey;
  elements.statsClientId.value = state.generatedKey;
  elements.revokeKeyInput.value = state.generatedKey;
  addLog('info', 'The latest generated key is now selected for requests and stats.');
}

function applyGeneratedKey(data) {
  state.generatedKey = data.apiKey || '';
  elements.generatedKeyValue.textContent = state.generatedKey || 'No key returned';
  elements.generatedKeyMessage.textContent = data.message || 'API key generated successfully.';
  elements.apiKeyInput.value = state.generatedKey;
  elements.statsClientId.value = state.generatedKey;
  elements.revokeKeyInput.value = state.generatedKey;
}

function populateStats(data) {
  if (!data) {
    addLog('error', 'No stats were returned by the backend.');
    return;
  }

  const setText = (id, value) => {
    document.getElementById(id).textContent = value ?? '-';
  };

  setText('statsClientValue', data.clientId);
  setText('statsTokens', data.liveTokens ?? data.liveTokens ?? data.getliveTokens ?? '-');
  setText('statsRefill', data.refillRate);
  setText('statsBaseRefill', data.baseRefillRate);
  setText('statsTotal', data.totalRequest);
  setText('statsRejected', data.rejectedRequest);
  setText('statsClean', data.cleanWindowCount);
  setText('statsCheckedAt', data.checkedAt);

  state.statsHistory.push({
    label: formatTimeLabel(),
    tokens: Number(data.liveTokens ?? data.getliveTokens ?? 0),
    rejected: Number(data.rejectedRequest ?? 0),
  });

  if (state.statsHistory.length > 10) {
    state.statsHistory.shift();
  }

  tokensChart.data.labels = state.statsHistory.map((entry) => entry.label);
  tokensChart.data.datasets[0].data = state.statsHistory.map((entry) => entry.tokens);
  tokensChart.data.datasets[1].data = state.statsHistory.map((entry) => entry.rejected);
  tokensChart.update();
}

async function request(url, options) {
  const startedAt = performance.now();
  let response;

  try {
    response = await fetch(url, options);
  } catch (error) {
    const elapsedMs = Math.round(performance.now() - startedAt);
    throw {
      message: `${error.message}. If you opened this page directly as a file, serve it from a local web server or the backend host to avoid browser CORS restrictions.`,
      elapsedMs,
    };
  }

  const elapsedMs = Math.round(performance.now() - startedAt);
  const rawText = await response.text();
  const data = tryParse(rawText);

  if (!response.ok) {
    throw {
      message: data?.message || `Request failed with status ${response.status}.`,
      status: response.status,
      data,
      elapsedMs,
    };
  }

  return {
    status: response.status,
    data,
    elapsedMs,
    url,
  };
}

function tryParse(rawText) {
  if (!rawText) {
    return {};
  }

  try {
    return JSON.parse(rawText);
  } catch {
    return rawText;
  }
}

function updateSession(status) {
  state.session.requests += 1;
  state.session.lastStatus = String(status);

  const success = Number(status) >= 200 && Number(status) < 300;
  if (success) {
    state.session.success += 1;
  } else {
    state.session.blocked += 1;
  }

  state.session.history.push({
    ok: success ? 1 : 0,
    fail: success ? 0 : 1,
  });

  if (state.session.history.length > 12) {
    state.session.history.shift();
  }

  elements.summaryRequests.textContent = String(state.session.requests);
  elements.summarySuccess.textContent = String(state.session.success);
  elements.summaryBlocked.textContent = String(state.session.blocked);
  elements.summaryLastStatus.textContent = String(status);

  const okSeries = new Array(12).fill(0);
  const failSeries = new Array(12).fill(0);
  const startIndex = 12 - state.session.history.length;

  state.session.history.forEach((entry, index) => {
    okSeries[startIndex + index] = entry.ok;
    failSeries[startIndex + index] = entry.fail;
  });

  sessionChart.data.datasets[0].data = okSeries;
  sessionChart.data.datasets[1].data = failSeries;
  sessionChart.update();
}

function updateLatestResponse(result) {
  elements.payloadStatus.textContent = `${result.status} in ${result.elapsedMs} ms`;
  elements.responsePreview.textContent =
    typeof result.data === 'string' ? result.data : JSON.stringify(result.data, null, 2);
}

function addLog(type, message) {
  const emptyState = elements.activityLog.querySelector('.empty-state');
  if (emptyState) {
    emptyState.remove();
  }

  const item = document.createElement('div');
  item.className = `log-item ${type}`;
  item.textContent = `[${new Date().toLocaleTimeString('en-IN', { hour12: false })}] ${message}`;
  elements.activityLog.prepend(item);
}

function clearLog() {
  elements.activityLog.innerHTML = '<div class="empty-state">Your request history will appear here.</div>';
  elements.responsePreview.textContent = 'Waiting for a request...';
  elements.payloadStatus.textContent = 'No response yet';
}

function shorten(value) {
  if (!value || value.length < 16) {
    return value;
  }

  return `${value.slice(0, 8)}...${value.slice(-6)}`;
}

function formatTimeLabel() {
  return new Date().toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

function pause(ms) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}

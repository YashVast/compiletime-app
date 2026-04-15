/**
 * CompileTime — Background Service Worker
 *
 * This is the brain of the Chrome Extension. It runs persistently in the
 * background and does two things:
 *  1. Maintains a WebSocket connection to the companion app
 *  2. Relays TRIGGER/DONE/ABORT events to the active tab's content script
 *
 * It never touches the DOM — that's the content script's job.
 */

const WS_URL = 'ws://localhost:9999/ws';
const RECONNECT_DELAY_MS = 3000;

let socket = null;
let reconnectTimer = null;

// ─── WebSocket Lifecycle ────────────────────────────────────────────────────

function connect() {
  console.log('[CompileTime] Connecting to companion app...');

  socket = new WebSocket(WS_URL);

  socket.onopen = () => {
    console.log('[CompileTime] Connected to companion app');
    clearTimeout(reconnectTimer);
  };

  socket.onmessage = (event) => {
    let data;
    try {
      data = JSON.parse(event.data);
    } catch (e) {
      console.error('[CompileTime] Could not parse message from companion:', event.data);
      return;
    }

    console.log('[CompileTime] Received event:', data);
    handleCompanionEvent(data);
  };

  socket.onclose = (event) => {
    console.log('[CompileTime] Disconnected from companion app — will retry in 3s');
    scheduleReconnect();
  };

  socket.onerror = (error) => {
    // onclose will fire right after — reconnect is handled there
    console.warn('[CompileTime] WebSocket error (companion app may not be running)');
  };
}

function scheduleReconnect() {
  clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(connect, RECONNECT_DELAY_MS);
}

// ─── Event Handling ─────────────────────────────────────────────────────────

/**
 * Routes events received from the companion app to the correct handler.
 * Event shapes are defined in ARCHITECTURE.md §2.5
 */
function handleCompanionEvent(event) {
  switch (event.type) {
    case 'TRIGGER':
      onTrigger(event);
      break;
    case 'DONE':
      onDone(event);
      break;
    case 'ABORT':
      onAbort();
      break;
    default:
      console.warn('[CompileTime] Unknown event type:', event.type);
  }
}

async function onTrigger(event) {
  console.log('[CompileTime] TRIGGER received — showing quiz for:', event.cmd);
  await messageActiveTab({ action: 'SHOW_QUIZ', cmd: event.cmd });
}

async function onDone(event) {
  console.log('[CompileTime] DONE received — dismissing quiz');
  await messageActiveTab({ action: 'HIDE_QUIZ', exitCode: event.exitCode, durationMs: event.durationMs });
}

async function onAbort() {
  console.log('[CompileTime] ABORT received — dismissing quiz');
  await messageActiveTab({ action: 'HIDE_QUIZ' });
}

// ─── Tab Messaging ──────────────────────────────────────────────────────────

/**
 * Sends a message to the currently active tab's content script.
 * The content script receives this and shows/hides the quiz overlay.
 */
async function messageActiveTab(message) {
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab) {
      console.warn('[CompileTime] No active tab found');
      return;
    }
    await chrome.tabs.sendMessage(tab.id, message);
  } catch (e) {
    // Tab may not have a content script (e.g. chrome:// pages) — ignore
    console.warn('[CompileTime] Could not message active tab:', e.message);
  }
}

// ─── Answer Forwarding ──────────────────────────────────────────────────────

/**
 * The content script sends answer results here.
 * We forward them to the companion app via WebSocket.
 */
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'ANSWER') {
    const payload = JSON.stringify({
      type: 'ANSWER',
      questionId: message.questionId,
      correct: message.correct,
      timeMs: message.timeMs,
    });

    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(payload);
      console.log('[CompileTime] Forwarded answer to companion:', payload);
    } else {
      console.warn('[CompileTime] Cannot forward answer — not connected to companion');
    }
  }
});

// ─── Boot ───────────────────────────────────────────────────────────────────

connect();

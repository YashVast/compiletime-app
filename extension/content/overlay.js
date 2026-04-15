/**
 * CompileTime — Content Script (Quiz Overlay)
 *
 * Injected into every tab. Sits dormant until the service worker sends a
 * SHOW_QUIZ message, then injects the quiz overlay into the page DOM.
 *
 * This script never touches WebSocket — all backend communication goes
 * through the service worker.
 */

const COMPANION_BASE = 'http://localhost:9999';
const QUIZ_TIME_SECONDS = 45;

let overlayEl = null;
let timerInterval = null;
let currentQuestion = null;

// ─── Message Listener ────────────────────────────────────────────────────────

chrome.runtime.onMessage.addListener((message) => {
  if (message.action === 'SHOW_QUIZ') {
    showQuiz(message.cmd);
  } else if (message.action === 'HIDE_QUIZ') {
    hideQuiz();
  }
});

// ─── Show / Hide ─────────────────────────────────────────────────────────────

async function showQuiz(cmd) {
  // Don't show if user is actively typing in an input/textarea
  const focused = document.activeElement;
  if (focused && (focused.tagName === 'INPUT' || focused.tagName === 'TEXTAREA' || focused.isContentEditable)) {
    console.log('[CompileTime] User is typing — skipping quiz');
    return;
  }

  if (overlayEl) return; // already showing

  let question;
  try {
    question = await fetchQuestion();
  } catch (e) {
    console.error('[CompileTime] Could not fetch question:', e);
    return;
  }

  currentQuestion = question;
  overlayEl = buildOverlay(question, cmd);
  document.body.appendChild(overlayEl);
  startTimer(QUIZ_TIME_SECONDS);
}

function hideQuiz() {
  if (!overlayEl) return;

  overlayEl.classList.add('ct-hiding');
  setTimeout(() => {
    overlayEl?.remove();
    overlayEl = null;
    currentQuestion = null;
    clearInterval(timerInterval);
  }, 250);
}

// ─── Question Fetching ───────────────────────────────────────────────────────

async function fetchQuestion() {
  const res = await fetch(`${COMPANION_BASE}/api/questions`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

// ─── Overlay Builder ─────────────────────────────────────────────────────────

function buildOverlay(question, cmd) {
  const el = document.createElement('div');
  el.id = 'compiletime-overlay';
  el.innerHTML = `
    <div id="ct-header">
      <span id="ct-title">⚡ CompileTime</span>
      <span id="ct-timer">${QUIZ_TIME_SECONDS}s</span>
    </div>
    <div id="ct-body">
      <div id="ct-command">$ ${cmd || 'build running...'}</div>
      <div id="ct-question">${escapeHtml(question.question)}</div>
      <div id="ct-options">
        ${question.options.map((opt, i) => `
          <button class="ct-option" data-index="${i}">${escapeHtml(opt)}</button>
        `).join('')}
      </div>
      <div id="ct-feedback"></div>
      <div id="ct-xp"></div>
    </div>
  `;

  // Wire up option clicks
  el.querySelectorAll('.ct-option').forEach(btn => {
    btn.addEventListener('click', () => onAnswer(parseInt(btn.dataset.index)));
  });

  return el;
}

// ─── Answer Handling ─────────────────────────────────────────────────────────

function onAnswer(selectedIndex) {
  if (!currentQuestion) return;

  clearInterval(timerInterval);

  const correct = selectedIndex === currentQuestion.correctIndex;
  const xpAwarded = correct ? currentQuestion.xp : 2;
  const timeMs = (QUIZ_TIME_SECONDS - parseInt(document.getElementById('ct-timer').textContent)) * 1000;

  // Visually mark options
  overlayEl.querySelectorAll('.ct-option').forEach((btn, i) => {
    btn.disabled = true;
    if (i === currentQuestion.correctIndex) btn.classList.add('ct-correct');
    else if (i === selectedIndex && !correct) btn.classList.add('ct-incorrect');
  });

  // Show explanation
  const feedback = document.getElementById('ct-feedback');
  feedback.textContent = currentQuestion.explanation;
  feedback.className = `ct-feedback ct-show ${correct ? 'ct-correct-fb' : 'ct-incorrect-fb'}`;

  // Show XP
  const xpEl = document.getElementById('ct-xp');
  xpEl.textContent = correct ? `+${xpAwarded} XP ✓` : `+${xpAwarded} XP`;
  xpEl.classList.add('ct-show');

  // Report result to service worker → companion
  chrome.runtime.sendMessage({
    action: 'ANSWER',
    questionId: currentQuestion.id,
    correct,
    timeMs,
  });
}

// ─── Timer ───────────────────────────────────────────────────────────────────

function startTimer(seconds) {
  let remaining = seconds;
  const timerEl = document.getElementById('ct-timer');

  timerInterval = setInterval(() => {
    remaining--;
    if (timerEl) {
      timerEl.textContent = `${remaining}s`;
      if (remaining <= 10) timerEl.classList.add('ct-urgent');
    }

    if (remaining <= 0) {
      clearInterval(timerInterval);
      onTimeout();
    }
  }, 1000);
}

function onTimeout() {
  if (!currentQuestion) return;

  // Reveal correct answer on timeout
  overlayEl?.querySelectorAll('.ct-option').forEach((btn, i) => {
    btn.disabled = true;
    if (i === currentQuestion.correctIndex) btn.classList.add('ct-correct');
  });

  const feedback = document.getElementById('ct-feedback');
  if (feedback) {
    feedback.textContent = `Time's up! ${currentQuestion.explanation}`;
    feedback.className = 'ct-feedback ct-show ct-incorrect-fb';
  }

  chrome.runtime.sendMessage({
    action: 'ANSWER',
    questionId: currentQuestion.id,
    correct: false,
    timeMs: QUIZ_TIME_SECONDS * 1000,
  });
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

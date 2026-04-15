const COMPANION_BASE = 'http://localhost:9999';

async function loadStats() {
  try {
    const res = await fetch(`${COMPANION_BASE}/api/stats`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const stats = await res.json();

    document.getElementById('xp').textContent = stats.totalXp ?? 0;
    document.getElementById('answered').textContent = stats.questionsAnswered ?? 0;

    const statusEl = document.getElementById('status');
    statusEl.textContent = '● Companion connected';
    statusEl.classList.add('connected');
  } catch (e) {
    document.getElementById('status').textContent = '○ Companion not running';
  }
}

loadStats();

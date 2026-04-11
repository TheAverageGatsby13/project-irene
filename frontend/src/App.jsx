import { useState } from 'react';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1';

const initialTemplateForm = {
  title: 'College Stress & Anxiety',
  description: 'Therapist-led support for anxiety, overthinking, and social pressure.',
  issueFocus: 'anxiety,stress,social-anxiety',
  goalFocus: 'confidence,regulation,routine',
  targetSize: 8,
  scheduleWindow: 'mon-19:00,wed-19:00',
  languageCode: 'en',
  region: 'IN',
  targetAgeBand: '18-24',
  minGroupSharingComfort: 'MEDIUM'
};

function App() {
  const [auth, setAuth] = useState(null);
  const [status, setStatus] = useState('Welcome to Project Irene.');

  return (
    <div className="app-shell">
      <div className="ambient-glow glow-left" />
      <div className="ambient-glow glow-right" />
      <header className="hero fade-up">
        <p className="eyebrow">Project Irene</p>
        <h1>AI-assisted cohorting for affordable group therapy</h1>
        <p className="hero-subtitle">
          Therapist approval required, with crisis-risk exclusion from regular matching.
        </p>
      </header>

      {!auth && (
        <LoginCard
          onAuthenticated={setAuth}
          setStatus={setStatus}
        />
      )}

      {auth && (
        <div className="workspace fade-up delay-1">
          <div className="workspace-header card">
            <div>
              <h2>Signed in as {auth.email}</h2>
              <p className="muted">Role: {auth.role}</p>
            </div>
            <button
              className="btn secondary"
              onClick={() => {
                setAuth(null);
                setStatus('Logged out.');
              }}
            >
              Logout
            </button>
          </div>

          {auth.role === 'PATIENT' && (
            <PatientIntake token={auth.token} setStatus={setStatus} />
          )}
          {(auth.role === 'THERAPIST' || auth.role === 'ADMIN') && (
            <TherapistDashboard token={auth.token} setStatus={setStatus} />
          )}
        </div>
      )}

      <footer className="status-bar card fade-up delay-2">{status}</footer>
    </div>
  );
}

function LoginCard({ onAuthenticated, setStatus }) {
  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('therapist@irene.app');
  const [password, setPassword] = useState('Password@123');
  const [role, setRole] = useState('PATIENT');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setStatus('Authenticating...');

    try {
      const endpoint = mode === 'login' ? '/auth/login' : '/auth/register';
      const payload = mode === 'login' ? { email, password } : { email, password, role };
      const res = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const body = await res.json();
      if (!res.ok) {
        throw new Error(body.message || 'Request failed');
      }
      onAuthenticated(body);
      setStatus('Authentication successful.');
    } catch (err) {
      setStatus(`Auth failed: ${err.message}`);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card auth-card fade-up delay-1">
      <h2>{mode === 'login' ? 'Login' : 'Register'}</h2>
      <form onSubmit={submit} className="form-grid">
        <label>
          Email
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        </label>
        <label>
          Password
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
        </label>
        {mode === 'register' && (
          <label>
            Role
            <select value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="PATIENT">PATIENT</option>
              <option value="THERAPIST">THERAPIST</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </label>
        )}
        <button className="btn" disabled={busy} type="submit">
          {busy ? 'Please wait...' : mode === 'login' ? 'Login' : 'Register'}
        </button>
      </form>
      <button
        className="link-btn"
        type="button"
        onClick={() => setMode((prev) => (prev === 'login' ? 'register' : 'login'))}
      >
        {mode === 'login' ? 'Need an account? Register' : 'Already have an account? Login'}
      </button>
    </section>
  );
}

function PatientIntake({ token, setStatus }) {
  const [form, setForm] = useState({
    ageBand: '18-24',
    languageCode: 'en',
    timezone: 'Asia/Kolkata',
    region: 'IN',
    availabilitySlots: 'mon-19:00,wed-19:00',
    groupSharingComfort: 'MEDIUM',
    structuredAnswers: '{"stress":"high","sleep":"medium"}',
    freeText: 'I feel anxiety in social settings and loneliness in college life.',
    goals: 'confidence,connection',
    consentAi: true,
    consentGroup: true,
    notEmergencyAck: true
  });
  const [response, setResponse] = useState(null);
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setStatus('Submitting intake and running AI pipeline...');
    try {
      const payload = {
        ...form,
        availabilitySlots: toArray(form.availabilitySlots),
        goals: toArray(form.goals)
      };
      const res = await fetch(`${API_BASE}/intake/submissions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });
      const body = await res.json();
      if (!res.ok) {
        throw new Error(body.message || 'Intake submission failed');
      }
      setResponse(body);
      setStatus('Intake submitted successfully.');
    } catch (err) {
      setStatus(`Intake failed: ${err.message}`);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card">
      <h2>Patient Intake</h2>
      <p className="muted">This flow captures consent, normalizes concerns, scores risk, and stores matching features.</p>
      <form onSubmit={submit} className="form-grid two-col">
        <label>
          Age Band
          <input value={form.ageBand} onChange={(e) => setForm({ ...form, ageBand: e.target.value })} required />
        </label>
        <label>
          Language
          <input value={form.languageCode} onChange={(e) => setForm({ ...form, languageCode: e.target.value })} required />
        </label>
        <label>
          Timezone
          <input value={form.timezone} onChange={(e) => setForm({ ...form, timezone: e.target.value })} required />
        </label>
        <label>
          Region
          <input value={form.region} onChange={(e) => setForm({ ...form, region: e.target.value })} required />
        </label>
        <label>
          Availability (comma-separated)
          <input value={form.availabilitySlots} onChange={(e) => setForm({ ...form, availabilitySlots: e.target.value })} required />
        </label>
        <label>
          Sharing Comfort
          <select value={form.groupSharingComfort} onChange={(e) => setForm({ ...form, groupSharingComfort: e.target.value })}>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
          </select>
        </label>
        <label className="full-width">
          Structured Answers JSON
          <textarea rows={3} value={form.structuredAnswers} onChange={(e) => setForm({ ...form, structuredAnswers: e.target.value })} />
        </label>
        <label className="full-width">
          Free Text Concern
          <textarea rows={4} value={form.freeText} onChange={(e) => setForm({ ...form, freeText: e.target.value })} required />
        </label>
        <label className="full-width">
          Goals (comma-separated)
          <input value={form.goals} onChange={(e) => setForm({ ...form, goals: e.target.value })} required />
        </label>
        <label className="check"><input type="checkbox" checked={form.consentAi} onChange={(e) => setForm({ ...form, consentAi: e.target.checked })} /> AI-assisted matching consent</label>
        <label className="check"><input type="checkbox" checked={form.consentGroup} onChange={(e) => setForm({ ...form, consentGroup: e.target.checked })} /> Group-therapy participation consent</label>
        <label className="check"><input type="checkbox" checked={form.notEmergencyAck} onChange={(e) => setForm({ ...form, notEmergencyAck: e.target.checked })} /> Not-for-emergency acknowledgement</label>
        <button className="btn full-width" type="submit" disabled={busy}>{busy ? 'Submitting...' : 'Submit Intake'}</button>
      </form>

      {response && (
        <div className="result-box">
          <h3>AI Output</h3>
          <p><strong>Risk:</strong> {response.riskLevel}</p>
          <p><strong>Issue Tags:</strong> {response.issueTags.join(', ')}</p>
          <p><strong>Goals:</strong> {response.goals.join(', ')}</p>
          <p><strong>Summary:</strong> {response.summary}</p>
          {response.routedToManualReview && <p className="alert">High-risk intake routed to manual review and excluded from normal matching.</p>}
        </div>
      )}
    </section>
  );
}

function TherapistDashboard({ token, setStatus }) {
  const [templateForm, setTemplateForm] = useState(initialTemplateForm);
  const [templateId, setTemplateId] = useState('1');
  const [runResult, setRunResult] = useState(null);
  const [selectedPatients, setSelectedPatients] = useState([]);
  const [groupTitle, setGroupTitle] = useState('Anxiety Cohort - Week 1');

  async function createTemplate(e) {
    e.preventDefault();
    setStatus('Creating program template...');
    try {
      const payload = {
        ...templateForm,
        issueFocus: toArray(templateForm.issueFocus),
        goalFocus: toArray(templateForm.goalFocus),
        scheduleWindow: toArray(templateForm.scheduleWindow),
        targetSize: Number(templateForm.targetSize)
      };
      const res = await api('/therapist/templates', token, {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      setTemplateId(String(res.id));
      setStatus(`Template created with id ${res.id}.`);
    } catch (err) {
      setStatus(`Template creation failed: ${err.message}`);
    }
  }

  async function runMatch() {
    setStatus('Running matching engine...');
    try {
      const res = await api(`/matching/run/${templateId}`, token, { method: 'POST' });
      setRunResult(res);
      setSelectedPatients([]);
      setStatus(`Matching run ${res.runId} completed with ${res.candidates.length} candidates.`);
    } catch (err) {
      setStatus(`Matching failed: ${err.message}`);
    }
  }

  async function createGroup() {
    if (!runResult || selectedPatients.length === 0) {
      setStatus('Select at least one candidate before creating group.');
      return;
    }
    setStatus('Creating therapist-approved group...');
    try {
      const res = await api('/groups/create-from-match', token, {
        method: 'POST',
        body: JSON.stringify({
          templateId: Number(templateId),
          runId: runResult.runId,
          title: groupTitle,
          patientIds: selectedPatients
        })
      });
      setStatus(`Group ${res.groupId} created with ${res.patientIds.length} invited members.`);
    } catch (err) {
      setStatus(`Group creation failed: ${err.message}`);
    }
  }

  function togglePatient(id) {
    setSelectedPatients((prev) =>
      prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id]
    );
  }

  return (
    <div className="dashboard-grid">
      <section className="card">
        <h2>Therapist Program Template</h2>
        <form className="form-grid" onSubmit={createTemplate}>
          {Object.entries(templateForm).map(([key, value]) => (
            <label key={key}>
              {humanize(key)}
              <input
                value={value}
                onChange={(e) => setTemplateForm({ ...templateForm, [key]: e.target.value })}
                required
              />
            </label>
          ))}
          <button className="btn" type="submit">Create Template</button>
        </form>
      </section>

      <section className="card">
        <h2>Run Matching</h2>
        <label>
          Template ID
          <input value={templateId} onChange={(e) => setTemplateId(e.target.value)} />
        </label>
        <button className="btn" onClick={runMatch}>Run Match</button>

        {runResult && (
          <>
            <p className="muted">Run ID: <code>{runResult.runId}</code></p>
            <div className="candidate-list">
              {runResult.candidates.map((candidate) => (
                <article key={candidate.patientId} className="candidate-card">
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={selectedPatients.includes(candidate.patientId)}
                      onChange={() => togglePatient(candidate.patientId)}
                    />
                    Select for cohort
                  </label>
                  <h3>{candidate.patientEmail}</h3>
                  <p><strong>Risk:</strong> {candidate.riskLevel}</p>
                  <p><strong>Score:</strong> {candidate.score.toFixed(3)}</p>
                  <p><strong>Issue Tags:</strong> {candidate.issueTags.join(', ')}</p>
                  <p className="muted">{candidate.explanation}</p>
                </article>
              ))}
            </div>

            <div className="group-actions">
              <label>
                Group Title
                <input value={groupTitle} onChange={(e) => setGroupTitle(e.target.value)} />
              </label>
              <button className="btn" onClick={createGroup}>Create Group from Selection</button>
            </div>
          </>
        )}
      </section>
    </div>
  );
}

function toArray(csv) {
  return csv
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

async function api(path, token, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(options.headers || {})
    }
  });
  const body = await res.json();
  if (!res.ok) {
    throw new Error(body.message || 'Request failed');
  }
  return body;
}

function humanize(value) {
  return value
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (char) => char.toUpperCase());
}

export default App;

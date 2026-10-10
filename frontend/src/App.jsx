import { useEffect, useState } from 'react';
import Workspace from './Workspace';
import CircleBloom from './CircleBloom';
import { resolveAccess } from './authRouting';

const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1';

function App() {
  const [auth, setAuth] = useState(null);
  const [status, setStatus] = useState('Explore Irene with fictional profiles only.');
  const [rawAiMode,setRawAiMode]=useState('mock');
  const [demo,setDemo]=useState(false);
  useEffect(() => { window.scrollTo({ top: 0, behavior: 'instant' }); }, [auth]);
  useEffect(() => {
    fetch(`${API_BASE}/health`).then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => { setRawAiMode(data.aiMode);setDemo(data.demoAccounts==='true'); })
      .catch(() => setStatus('We couldn’t connect to Irene. Please try again shortly.'));
  }, []);

  return (
    <div className={`app-shell ${auth ? 'signed-in' : 'landing'}`}>
      <a className="skip-link" href="#main-content">Skip to content</a>
      <nav className="topbar" aria-label="Main navigation">
        <a href="#" className="wordmark"><span className="brand-mark">i</span> irene<span className="brand-dot">.</span></a>
        <span className="nav-note">A little support. A shared beginning.</span>
        {!auth && <a className="nav-login" href="#your-space">Enter your space <span aria-hidden="true">↗</span></a>}
        <span className="demo-pill">Research prototype</span>
      </nav>
      {!auth && <>
      <main id="main-content">
      <header className="hero fade-up">
        <div className="hero-copy">
          <p className="eyebrow"><span className="sun-dot"/> SUPPORT, WITH ROOM FOR YOU</p>
          <h1>A little<br/>less <em>alone.</em></h1>
          <p className="hero-subtitle">Some things feel lighter when they’re shared. Find your way to therapist-led group support, at a cost you share together.</p>
          <div className="hero-actions"><a className="btn" href="#your-space">Find your circle <span aria-hidden="true">↗</span></a><a className="text-link" href="#how-it-works">How it works <span aria-hidden="true">↓</span></a></div>
          <div className="hero-footnote"><span className="tiny-circles" aria-hidden="true"><i/><i/><i/></span><span>Your story is yours.<br/>You choose the next step.</span></div>
        </div>
        <CircleBloom/>
      </header>
      <section className="journey-section" id="how-it-works" aria-labelledby="journey-heading">
        <div className="section-intro"><p className="eyebrow">A SHARED BEGINNING</p><h2 id="journey-heading">Small steps.<br/><em>Human connection.</em></h2><p>No need to have the perfect words.<br/>Start where you are.</p></div>
        <div className="journey" aria-label="How Irene works">
          <article><span>01</span><div><h3>A private first step</h3><p>Share what you need through a private intake. Other participants never see it.</p></div></article>
          <article><span>02</span><div><h3>A little common ground</h3><p>AI helps compare shared concerns and goals. Your language and availability help shape a suitable circle.</p></div></article>
          <article><span>03</span><div><h3>A human to guide you</h3><p>AI prepares a draft summary for you to check. A therapist reviews every group, and you choose whether to join.</p></div></article>
        </div>
      </section>
      <section className="shared-section" aria-labelledby="shared-heading">
        <div className="shared-copy"><p className="eyebrow">SHARE THE SESSION. SHARE THE COST.</p><h2 id="shared-heading">More connection.<br/><em>A lighter cost.</em></h2><p>One therapist-led session, with the fee divided across your circle. A simple idea that makes a first step feel a little more within reach.</p><span className="editorial-note">Together looks different for everyone.</span></div>
        <AffordabilityCard/>
      </section>
      </main>
      </>}
      {!auth && (
        <LoginCard
          demo={demo}
          onAuthenticated={setAuth}
          setStatus={setStatus}
        />
      )}

      {auth && <Workspace auth={auth} demo={demo} aiMode={rawAiMode} onLogout={()=>{setAuth(null);setStatus('Signed out.');}}/>}

      <footer className="status-bar" role="status" aria-live="polite">{status}</footer>
      <p className="footer-note">Irene is a prototype, not an emergency service. No one is monitoring this demo. Group members are asked to keep sessions private.</p>
    </div>
  );
}

function LoginCard({ onAuthenticated, setStatus, demo }) {
  const [mode, setMode] = useState('login');
  const [access, setAccess] = useState('participant');
  const [error, setError] = useState('');
  const accessLabel = { participant: 'Participant', therapist: 'Therapist', admin: 'Admin' }[access];
  function chooseAccess(next) { setAccess(next); setMode('login'); setError(''); }
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    setStatus('Authenticating...');

    try {
      const endpoint = mode === 'login' ? '/auth/login' : '/auth/register';
      const payload = mode === 'login' ? { email, password } : { email, password, role: 'PATIENT' };
      const res = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const body = await readResponse(res);
      if (!res.ok) {
        throw new Error(body.message || 'Request failed');
      }
      onAuthenticated({ ...body, ...resolveAccess(access, body.role) });
      setStatus('Authentication successful.');
    } catch (err) {
      setError(err.message);
      setStatus('Sign-in needs your attention.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section id="your-space" className="auth-section fade-up delay-1" aria-labelledby="auth-heading">
      <div className="auth-aside"><span className="eyebrow">A SPACE TO BE YOURSELF</span><h2>Your next chapter<br/>starts with a<br/><em>small hello.</em></h2><p>Take a look around with a fictional profile. There’s room to explore at your own pace.</p><span className="auth-flower" aria-hidden="true">✳</span></div>
      <div className="auth-card">
      <p className="eyebrow">YOUR NEXT SMALL STEP</p>
      <h2 id="auth-heading">{mode === 'login' ? { participant: 'Welcome to your space', therapist: 'Your therapist space', admin: 'Irene administration' }[access] : access === 'therapist' ? 'Begin your therapist application' : 'Create a participant account'}</h2>
      <div className="access-switch" role="group" aria-label="Choose your sign-in space">
        {['participant', 'therapist', 'admin'].map(value => <button key={value} type="button" aria-pressed={access === value} disabled={busy} onClick={() => chooseAccess(value)}>{ { participant: 'Participant', therapist: 'Therapist', admin: 'Admin' }[value]}</button>)}
      </div>
      <p className="access-help muted">{access === 'admin' ? 'Sign in with your administrator account. Admin access is assigned privately; public registration does not grant it.' : access === 'therapist' ? 'Approved therapists can sign in here. New professionals can create an account and submit their qualifications for administrator review.' : 'Sign in or create an account using fictional details. Your account opens the workspace for its approved role.'}</p>
      {error && <div className="notice error" role="alert">{error}</div>}
      {mode === 'login' && demo && <div className="chip-row">
        <button className="chip" onClick={() => { chooseAccess('participant'); setEmail('patient1@irene.app'); setPassword('Password@123'); }}>Participant demo</button>
        <button className="chip" onClick={() => { chooseAccess('therapist'); setEmail('therapist@irene.app'); setPassword('Password@123'); }}>Therapist demo</button>
        <button className="chip" onClick={() => { chooseAccess('admin'); setEmail('admin@irene.app'); setPassword('Password@123'); }}>Administrator demo</button>
      </div>}
      <form onSubmit={submit} className="form-grid">
        <label>
          Email
          <input autoComplete="email" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        </label>
        <label>
          Password
          <input autoComplete={mode === 'login' ? 'current-password' : 'new-password'} placeholder="Your password" value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
        </label>
        <button className="btn" disabled={busy} type="submit">
          {busy ? 'Please wait...' : mode === 'login' ? `Sign in · ${accessLabel}` : access === 'therapist' ? 'Create account & apply' : 'Create account'}
        </button>
      </form>
      {access !== 'admin' && <button
        className="link-btn"
        disabled={busy}
        type="button"
        onClick={() => { setMode((prev) => (prev === 'login' ? 'register' : 'login')); setError(''); }}
      >
        {mode === 'login' ? access === 'therapist' ? 'New professional? Apply for therapist access' : 'Need an account? Register' : 'Already have an account? Sign in'}
      </button>}
      </div>
    </section>
  );
}

async function readResponse(res) {
  try { return await res.json(); }
  catch { throw new Error('The service could not respond. Please try again shortly.'); }
}

function AffordabilityCard() {
  const [members, setMembers] = useState(6);
  const fee = 2400;
  return <aside className="cost-card" aria-label="Illustrative shared session cost">
    <div className="cost-heading"><p className="eyebrow">THE TOGETHER EFFECT</p><span className="state-badge">Illustrative pricing</span></div>
    <div className="circle-art" aria-hidden="true">{Array.from({ length: members }, (_, i) => <span key={i} style={{ transform: `rotate(${i * 360 / members}deg) translateY(-53px)` }} />)}<b>{members}<small>in your circle</small></b></div>
    <div className="cost-number" aria-live="polite">₹{Math.ceil(fee / members)}<span>per person / session</span></div>
    <label className="cost-slider">{members} people sharing an illustrative ₹2,400 session
      <input aria-valuetext={`${members} people, ₹${Math.ceil(fee / members)} per person`} aria-label="Number of participants" type="range" min="4" max="8" value={members} onChange={e => setMembers(Number(e.target.value))} />
    </label>
    <p className="cost-caption">An example, not a quote. Actual fees, group size, and suitability are set with the therapist. Other charges may apply.</p>
  </aside>;
}

export default App;

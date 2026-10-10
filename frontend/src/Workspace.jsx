import { useEffect, useState } from 'react';
import CircleBloom from './CircleBloom';
import { request, money, list, saveFile, calendar } from './api';

const readableSlots = value => list(value || '').map(s => s.slice(0,3).replace(/^./,c=>c.toUpperCase()) + ' ' + s.slice(4)).join(' · ');
const Label = ({children,...props}) => <label className="field" {...props}>{children}</label>;
const Empty = ({children}) => <div className="empty-state"><span aria-hidden="true">◌</span><p>{children}</p></div>;
const Badge = ({children}) => <span className="state-badge">{String(Array.isArray(children) ? children.join('') : children || '').replaceAll('_', ' ').toLowerCase()}</span>;
const DateLabel = ({value}) => value ? <time dateTime={value}>{new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}</time> : 'Not scheduled';

export default function Workspace({auth, onLogout, demo, aiMode}) {
  const [data,setData] = useState(null), [tab,setTab] = useState(auth.initialTab || 'overview'), [busy,setBusy] = useState(false), [notice,setNotice] = useState(''), [error,setError] = useState('');
  const [program,setProgram] = useState('');
  const role = auth.role;
  const applying = role === 'PATIENT' && auth.professionalEntry;
  async function refresh() { const d = await request('/experience/workspace',auth.token); setData(d); if (!program && d.programs[0]) setProgram(String(d.intake?.programId || d.programs[0].id)); }
  useEffect(() => { refresh().catch(e => setError(e.message)); }, [auth.token]);
  async function act(fn, success) { setBusy(true); setError(''); setNotice(''); try { const r=await fn(); await refresh(); setNotice(success || r?.message || 'Saved.'); return r; } catch(e) { setError(e.message); return null; } finally { setBusy(false); } }
  const api = (path, method='GET', body) => request(path,auth.token,method,body);
  const tabs = applying ? [['credentials', 'Therapist application']] : role==='PATIENT' ? [['overview','My space'],['intake','Private intake'],['circles','My circles'],['privacy','Privacy & data'],['credentials','For professionals']]
    : role==='THERAPIST' ? [['overview','Overview'],['reviews','Intake review'],['matching','Build a circle'],['circles','My circles'],['programs','Programs']]
    : [['overview','Overview'],['credentials','Verify professionals']];
  return <div className="product-shell">
    <aside className="sidebar"><p className="eyebrow">{applying?'PROFESSIONAL ACCESS':role==='PATIENT'?'YOUR SPACE':role==='THERAPIST'?'FACILITATOR SPACE':'ADMINISTRATION'}</p>
      <nav aria-label="Workspace navigation">{tabs.map(([id,name])=><button key={id} className={tab===id?'nav-item selected':'nav-item'} aria-current={tab===id?'page':undefined} onClick={()=>{setTab(id);setError('');setNotice('');}}><span className="nav-label"><span className="nav-glyph" aria-hidden="true">{({overview:'◒',intake:'✎',circles:'◎',privacy:'◇',credentials:'✳',reviews:'▤',matching:'⊕',programs:'▦'})[id]}</span>{name}</span>{id==='reviews'&&data?.reviews.filter(r=>r.status==='PENDING').length>0&&<span className="count">{data.reviews.filter(r=>r.status==='PENDING').length}</span>}</button>)}</nav>
      <div className="sidebar-bottom"><span>{auth.email}</span><button className="link-btn" onClick={onLogout}>Sign out</button></div>
    </aside>
    <main className="product-main" id="main-content">
      <div className="workspace-title"><div><p className="eyebrow">SMALL STEPS, MEANINGFUL SUPPORT</p><h1>{tabs.find(([id])=>id===tab)?.[1]}</h1></div><button className="btn secondary" onClick={()=>act(refresh,'Up to date.')} disabled={busy}>Refresh</button></div>
      {error&&<div className="notice error" role="alert">{error}</div>}{notice&&<div className="notice" role="status">{notice}</div>}
      {!data ? <Empty>{error?'The workspace could not load. Use Refresh to try again.':'Opening your space…'}</Empty> : <>
        {tab==='overview'&&<>
          <section className="welcome-panel"><div><p className="eyebrow">{role==='PATIENT'?'A PLACE TO BEGIN':'CARE, WITH HUMAN JUDGMENT'}</p><h2>{role==='PATIENT'?'Your pace. Your choice.':'Make room for meaningful connection.'}</h2><p>{role==='PATIENT'?'Choose a circle, tell us what you need privately, and check your summary before a therapist sees it.':'Review each person, understand the group, and make the final decision. Irene helps you prepare.'}</p><button className="btn" onClick={()=>setTab(role==='PATIENT'?'intake':role==='THERAPIST'?'reviews':'credentials')}>{role==='PATIENT'?(data.intake?'Continue my intake':'Start private intake'):'Open review queue'} →</button></div><CircleBloom compact/></section>
          <div className="metric-grid"><article><span>{role==='PATIENT'?'Intake status':'Awaiting review'}</span><strong>{role==='PATIENT'?(data.intake?(data.intake.confirmed?data.intake.reviewStatus.replaceAll('_',' '):'CHECK SUMMARY'):'NOT STARTED'):data.reviews.filter(r=>r.status==='PENDING').length}</strong></article><article><span>My circles</span><strong>{data.circles.length}</strong></article><article><span>Processing mode</span><strong>{aiMode==='google'?'Google AI':'Simulation'}</strong></article></div>
          {role==='PATIENT'&&data.intake&&<div className="card"><h3>Your next step</h3><p>{data.circles.some(c=>c.status==='ACTIVE'&&c.response==='CONFIRMED')?'Your place is confirmed. Open My circles to prepare and save your private calendar event.':!data.intake.confirmed?'Read and confirm the summary in Private intake.':data.intake.reviewStatus==='PENDING'?'Your summary is awaiting review by your chosen therapist. This demo is not continuously monitored.':data.intake.reviewStatus==='INDIVIDUAL'?'Your therapist recommends an individual conversation before a group. View their note in Private intake.':'Your therapist has approved consideration for a group. Check My circles for invitations.'}</p></div>}
          <div className="program-section-heading"><div><p className="eyebrow">FIND YOUR COMMON GROUND</p><h2 className="section-title">A circle for your chapter.</h2></div><span className="muted">Available programs</span></div><div className="program-grid">{data.programs.map((p,index)=><article className="card program-card" key={p.id}><span className="program-number" aria-hidden="true">{String(index+1).padStart(2,'0')}</span><Badge>{p.language} · {p.ageBand}</Badge><h3>{p.title}</h3><p>{p.description}</p><p className="muted">{p.therapist}<br/>{readableSlots(p.schedule)} · {p.timezone}<br/>Up to {p.capacity} participants</p>{role==='PATIENT'&&<button className="btn secondary" onClick={()=>{setProgram(String(p.id));setTab('intake');}}>Explore this program</button>}</article>)}</div>{!data.programs.length&&<Empty>No verified programs are available yet. An administrator must verify a professional before their program appears.</Empty>}
        </>}
        {tab==='intake'&&<IntakePanel data={data} program={program} setProgram={setProgram} api={api} act={act} busy={busy} demo={demo} aiMode={aiMode}/>}
        {tab==='reviews'&&<ReviewPanel reviews={data.reviews} api={api} act={act} busy={busy}/>}
        {tab==='matching'&&<MatchingPanel data={data} auth={auth} api={api} act={act} busy={busy} onCreated={()=>setTab('circles')}/>}
        {tab==='circles'&&<><p className="muted">{role==='PATIENT'?'Invitations appear only after therapist approval. Your intake is never shared with other participants.':'Approve a proposal to reveal private invitations in participants’ accounts. Fees are fixed when you approve.'}</p><div className="circle-grid">{data.circles.map(c=><CircleCard key={c.id} circle={c} therapist={role==='THERAPIST'} api={api} act={act} busy={busy} demo={demo}/>)}</div>{!data.circles.length&&<Empty>{role==='PATIENT'?'No invitations yet. Finish intake confirmation and therapist review first.':'No groups yet. Review participants, then use Build a circle.'}</Empty>}</>}
        {tab==='programs'&&<ProgramForm api={api} act={act} busy={busy}/>}
        {tab==='privacy'&&<PrivacyPanel api={api} act={act} busy={busy}/>}
        {tab==='credentials'&&<CredentialPanel credentials={data.credentials} admin={role==='ADMIN'} api={api} act={act} busy={busy}/>}
      </>}
      <p className="footer-note">{demo?'Fictional profiles and simulated sessions. ':''}Not an emergency service. No live crisis monitoring. Google Meet may reveal your account name; check your identity settings before joining. No recording is enabled by Irene.</p>
    </main>
  </div>;
}

function IntakePanel({data,program,setProgram,api,act,busy,demo,aiMode}) {
  const [stage,setStage]=useState(1),[concerns,setConcerns]=useState(''),[goals,setGoals]=useState(''),[summary,setSummary]=useState(data.intake?.summary||''),[budget,setBudget]=useState((data.intake?.budgetMinor ?? 60000)/100);
  const [age,setAge]=useState('18-24'),[slots,setSlots]=useState('mon-19:00,wed-19:00'),[comfort,setComfort]=useState('MEDIUM'),[consent,setConsent]=useState([false,false,false]);
  useEffect(()=>setSummary(data.intake?.summary||''),[data.intake?.id,data.intake?.summary]);
  const selected=data.programs.find(p=>String(p.id)===String(program));
  async function submit(e) { e.preventDefault(); if(!selected)return; const res=await act(()=>api('/intake/submissions','POST',{ageBand:age,languageCode:selected.language,timezone:selected.timezone,region:selected.region,availabilitySlots:list(slots).filter(s=>list(selected.schedule).includes(s)),groupSharingComfort:comfort,structuredAnswers:'{}',freeText:concerns,goals:list(goals),consentAi:consent[0],consentGroup:consent[1],notEmergencyAck:consent[2]}),'Draft ready. Check and correct your summary below.'); if(res)setStage(3); }
  const intake=data.intake;
  return <>
    <div className="steps" aria-label="Intake progress">{['Your preferences','Your story','Review & confirm'].map((label,i)=><button key={label} className={stage===i+1?'step current':'step'} aria-current={stage===i+1?'step':undefined} onClick={()=>setStage(i+1)}><span>{i+1}</span>{label}</button>)}</div>
    <section className="card intake-card"><p className="eyebrow">PRIVATE TO YOU AND YOUR ASSIGNED THERAPIST</p><h2>{stage===1?'Let’s find a practical fit.':stage===2?'What would you like support with?':'Does this reflect what you mean?'}</h2>
    {stage===1&&<div className="form-grid two-col">
      <Label>Program<select value={program} onChange={e=>setProgram(e.target.value)}>{data.programs.map(p=><option key={p.id} value={p.id}>{p.title}</option>)}</select></Label>
      <Label>Adult age band<select value={age} onChange={e=>setAge(e.target.value)}>{['18-24','25-34','35-44','45-54','55+'].map(x=><option key={x}>{x}</option>)}</select></Label>
      <Label>Maximum per-session budget (₹)<input type="number" min="0" max="100000" step="1" value={budget} onChange={e=>setBudget(e.target.value)}/></Label>
      <Label>Sharing comfort<select value={comfort} onChange={e=>setComfort(e.target.value)}><option value="LOW">I’d like to listen first</option><option value="MEDIUM">I can share a little</option><option value="HIGH">I’m comfortable sharing</option></select></Label>
      <fieldset className="availability full-width"><legend>Times that work for you · {selected?.timezone || 'program time zone'}</legend><div className="button-row">{list(selected?.schedule || '').map(slot=><label className="check" key={slot}><input type="checkbox" checked={list(slots).includes(slot)} onChange={e=>setSlots((e.target.checked?[...list(slots),slot]:list(slots).filter(s=>s!==slot)).join(','))}/>{readableSlots(slot)}</label>)}</div><small>Select at least one program time. Your session must fit this availability.</small></fieldset>
      <p className="muted full-width">Program language: {selected?.language || '—'}. Choosing a program determines which verified therapist can see the summary you confirm. An invitation will show the exact fee before you accept.</p>
      <button className="btn" onClick={()=>setStage(2)} disabled={!selected||!list(slots).some(s=>list(selected.schedule).includes(s))}>Continue to your story →</button>
    </div>}
    {stage===2&&<form className="form-grid" onSubmit={submit}>
      <p className="muted">Use fictional information only. Avoid names, phone numbers, institutions or identifying details. Your story is not visible to other participants.</p>
      {demo&&<button type="button" className="chip" onClick={()=>{setConcerns('This fictional student feels anxious speaking in class and lonely after moving to a new city.');setGoals('confidence,connection');}}>Use fictional example</button>}
      <Label>What has been on your mind?<textarea rows="5" maxLength="8000" required value={concerns} onChange={e=>setConcerns(e.target.value)}/></Label>
      <Label>What would you like to work toward?<input required maxLength="1000" value={goals} onChange={e=>setGoals(e.target.value)} placeholder="For example: confidence, connection"/></Label>
      {['I agree to AI processing of this fictional intake to prepare a draft and matching features.','I agree to my confirmed summary being reviewed by the chosen therapist for group consideration. I can decline an invitation.','I understand this prototype is not monitored for emergencies, and confidentiality between group participants cannot be guaranteed.'].map((text,i)=><label className="check" key={text}><input type="checkbox" required checked={consent[i]} onChange={e=>setConsent(consent.map((v,j)=>i===j?e.target.checked:v))}/>{text}</label>)}
      <div className="button-row"><button type="button" className="btn secondary" onClick={()=>setStage(1)}>Back</button><button className="btn" disabled={busy||!selected}>{busy?'Preparing your draft…':'Prepare my summary'}</button></div>
    </form>}
    {stage===3&&(!intake?<Empty>First share your fictional story to prepare a draft.</Empty>:<form className="form-grid" onSubmit={async e=>{e.preventDefault();await act(()=>api(`/experience/intakes/${intake.id}/confirm`,'PUT',{programId:Number(program),summary,budgetMinor:Math.round(Number(budget)*100)}));}}>
      <div className="button-row"><Badge>{intake.confirmed?'Confirmed':'Awaiting your confirmation'}</Badge><Badge>{intake.reviewStatus}</Badge></div>
      <p className="muted">{aiMode==='google'?'AI-generated':'Simulated'} draft. Correct anything that is inaccurate. Confirming shares it with the selected therapist and rechecks matching features. This is not a diagnosis.</p>
      {['HIGH','REVIEW_REQUIRED'].includes(intake.risk)&&<div className="notice error">This intake needs separate human review and will not enter routine group matching. This demo does not contact emergency services. If you need immediate help, contact local emergency services or someone you trust.</div>}
      <Label>Your summary<textarea rows="5" maxLength="1500" required value={summary} onChange={e=>setSummary(e.target.value)}/></Label>
      <p className="muted">Program: {selected?.title || 'Choose a program in preferences'} · Budget: {money(Math.round(Number(budget)*100))}</p>
      {intake.reviewNote&&<div className="review-note"><strong>Your therapist’s note</strong><p>{intake.reviewNote}</p></div>}
      <button className="btn" disabled={busy||!selected}>{busy?'Checking and saving…':intake.confirmed?'Save correction & request new review':'Confirm & request therapist review'}</button>
    </form>)}
    </section>
  </>;
}

function ReviewPanel({reviews,api,act,busy}) {
  const [filter,setFilter]=useState('PENDING');
  return <><div className="button-row">{['PENDING','APPROVED','INDIVIDUAL','ALL'].map(f=><button key={f} className={f===filter?'chip active':'chip'} onClick={()=>setFilter(f)}>{f.toLowerCase()}</button>)}</div><p className="muted">These participants chose one of your programs and confirmed their summaries. Approving means suitable for consideration, not automatic placement. Recommending individual review cancels any current circle that includes this intake.</p>
    {reviews.filter(r=>filter==='ALL'||r.status===filter).map(r=><ReviewCard key={r.intakeId} review={r} api={api} act={act} busy={busy}/>)}
    {!reviews.filter(r=>filter==='ALL'||r.status===filter).length&&<Empty>No intakes in this view.</Empty>}</>;
}
function ReviewCard({review:r,api,act,busy}) {
  const [note,setNote]=useState('');const restricted=['HIGH','REVIEW_REQUIRED'].includes(r.risk);
  return <article className="card review-card"><div className="card-heading"><h3>{r.participant}</h3><Badge>{r.status}</Badge></div><p className="muted">{r.program} · Budget {money(r.budgetMinor)}</p><blockquote>{r.summary}</blockquote><p><strong>Review signal:</strong> {r.risk.replaceAll('_',' ')} <small>Experimental indicator; not a diagnosis.</small></p>
    {restricted&&<p className="alert">Separate individual review required. Routine group approval is disabled.</p>}
    <Label>Review note for the participant<textarea value={note} onChange={e=>setNote(e.target.value)} maxLength="1000" placeholder="Record your rationale and the next step."/></Label>
    <div className="button-row"><button className="btn" disabled={busy||restricted||!note.trim()} onClick={()=>act(()=>api(`/experience/intakes/${r.intakeId}/review`,'PUT',{decision:'APPROVED',note}))}>Approve for group consideration</button><button className="btn secondary" disabled={busy||!note.trim()} onClick={()=>act(()=>api(`/experience/intakes/${r.intakeId}/review`,'PUT',{decision:'INDIVIDUAL',note}))}>Recommend individual review</button></div>
  </article>;
}
function MatchingPanel({data,auth,api,act,busy,onCreated}) {
  const [id,setId]=useState(''),[result,setResult]=useState(null),[selected,setSelected]=useState([]),[title,setTitle]=useState('A shared beginning');
  // Catalog selection is public; server ownership checks remain authoritative.
  async function run() { setResult(null);setSelected([]);const r=await act(()=>api(`/matching/run/${id}`,'POST'),'Matching complete. Review the suggestions before proposing a group.');if(r)setResult(r); }
  return <section className="card"><h2>Find common ground</h2><p className="muted">Only current participant-confirmed, therapist-approved intakes can appear. Language, region, age band, program time zone, availability and sharing preference must fit.</p>
    <Label>Program<select value={id} onChange={e=>{setId(e.target.value);setResult(null);setSelected([]);}}><option value="">Choose a program</option>{data.programs.filter(p=>p.therapistId===auth.userId).map(p=><option value={p.id} key={p.id}>{p.title} · #{p.id}</option>)}</select></Label>
    <button className="btn" disabled={busy||!id} onClick={run}>Find reviewed participants</button>
    {result&&<><p className="muted">{result.candidates.length} suggestions. Scores describe matching signals, not clinical suitability.</p><div className="candidate-grid">{result.candidates.map(c=><label className={'candidate-choice '+(selected.includes(c.patientId)?'chosen':'')} key={c.patientId}><input type="checkbox" checked={selected.includes(c.patientId)} onChange={()=>setSelected(selected.includes(c.patientId)?selected.filter(x=>x!==c.patientId):[...selected,c.patientId])}/><div><h3>Participant {c.patientId}</h3><p>{c.issueTags.join(' · ')}</p><p className="muted">{c.explanation}</p></div></label>)}</div>{!result.candidates.length&&<Empty>Review and approve suitable intakes for this program first.</Empty>}
      <Label>Circle title<input maxLength="150" value={title} onChange={e=>setTitle(e.target.value)}/></Label><button className="btn" disabled={busy||selected.length<2||!title.trim()} onClick={async()=>{const r=await act(()=>api('/groups/create-from-match','POST',{templateId:result.templateId,runId:result.runId,title,patientIds:selected}),'Proposal saved. Set the schedule and approve it in My circles.');if(r)onCreated();}}>Save proposal for {selected.length} people</button></>}
  </section>;
}
function CircleCard({circle:c,therapist,api,act,busy,demo}) {
  const [date,setDate]=useState(''),[fee,setFee]=useState(2400),[duration,setDuration]=useState(60),[url,setUrl]=useState(''),[reviewed,setReviewed]=useState(false),[rating,setRating]=useState(4),[comment,setComment]=useState('');
  const confirmed=c.response==='CONFIRMED';
  return <article className="card circle-card"><div className="card-heading"><Badge>{c.status==='ACTIVE'?'Invitations open':c.status}</Badge>{!therapist&&c.response&&<Badge>{c.response}</Badge>}</div><h2>{c.title}</h2><p className="muted">{c.program}<br/>{c.therapist}</p>
    <div className="session-facts"><div><small>Session</small><strong><DateLabel value={c.startsAt}/></strong></div><div><small>{therapist?'Per-person quote':'Your fee'} · no payment collected</small><strong>{c.priceMinor===null?'Quoted on approval':money(c.priceMinor)}</strong></div><div><small>Group</small><strong>{c.accepted} accepted / {c.invited} invited</strong></div></div>
    {therapist&&<><details><summary>Participant responses</summary><ul>{c.members.map(m=><li key={m.participantId}>{m.alias} — {m.status.toLowerCase()}</li>)}</ul></details>
      {c.status==='PENDING_APPROVAL'&&<form className="form-grid approval-form" onSubmit={e=>{e.preventDefault();act(()=>api(`/experience/circles/${c.id}/approve`,'POST',{startsAt:new Date(date).toISOString(),durationMinutes:Number(duration),totalFeeMinor:Math.round(Number(fee)*100),meetingUrl:url,suitabilityConfirmed:reviewed}));}}>
        <h3>Schedule and approve</h3><p className="muted">Program times: {readableSlots(c.schedule)} · {c.timezone}. The chosen time must also fit every participant’s availability.</p><Label>Start time in your device’s time zone<input type="datetime-local" required value={date} onChange={e=>setDate(e.target.value)}/></Label><div className="form-grid two-col"><Label>Minutes<input type="number" min="30" max="120" required value={duration} onChange={e=>setDuration(e.target.value)}/></Label><Label>Total session fee (₹)<input type="number" min="0" max="100000" step="1" required value={fee} onChange={e=>setFee(e.target.value)}/></Label></div>
        <Label>Google Meet link<input type="url" required value={url} onChange={e=>setUrl(e.target.value)} placeholder="https://meet.google.com/xxx-xxxx-xxx"/></Label>
        <p className="muted">Create the meeting in your own Google Calendar. Irene stores your link; it does not create or verify a live room. Check participant identity and host privacy controls. The quote is fixed at {money(Math.ceil(Number(fee)*100/c.invited))} per person; it will not increase if someone declines.</p>
        <label className="check"><input type="checkbox" checked={reviewed} onChange={e=>setReviewed(e.target.checked)} required/>I reviewed each current intake and the suitability of this group, schedule and fee.</label><button className="btn" disabled={busy}>Approve & publish private invitations</button>
      </form>}
      {c.status!=='CANCELLED'&&<button className="btn secondary" disabled={busy} onClick={()=>act(()=>api(`/experience/circles/${c.id}/brief`,'POST'))}>Prepare facilitator draft</button>}
      {c.brief&&<details open><summary>Private facilitator draft</summary><p className="brief">{c.brief}</p></details>}
      {c.status==='ACTIVE'&&<button className="btn secondary" disabled={busy} onClick={()=>act(()=>api(`/experience/circles/${c.id}/complete`,'POST'))}>Mark ended session complete</button>}
      {!['CANCELLED','COMPLETED'].includes(c.status)&&<button className="link-btn danger" disabled={busy} onClick={()=>act(()=>api(`/experience/circles/${c.id}/cancel`,'POST'))}>Cancel this circle</button>}
    </>}
    {!therapist&&c.status==='ACTIVE'&&<div className="button-row">{!confirmed&&<button className="btn" disabled={busy} onClick={()=>act(()=>api(`/experience/circles/${c.id}/response`,'PUT',{response:'CONFIRMED'}))}>Accept invitation at {money(c.priceMinor)}</button>}<button className="btn secondary" disabled={busy} onClick={()=>act(()=>api(`/experience/circles/${c.id}/response`,'PUT',{response:'DECLINED'}))}>{confirmed?'Withdraw from this circle':'Decline invitation'}</button></div>}
    {(therapist||confirmed)&&c.status==='ACTIVE'&&<div className="session-prep"><h3>Before we meet</h3><ul><li>Find a private space and use headphones.</li><li>Check the name and account visible in Google Meet.</li><li>You may pass on a question. Do not record or share others’ stories.</li></ul><div className="button-row"><a className="btn" href={c.meetingUrl} target="_blank" rel="noreferrer">Open Google Meet ↗</a><button className="btn secondary" onClick={()=>calendar(c)}>Save private calendar event</button></div>{demo&&<small>Demo links are illustrative unless you provided a real room.</small>}</div>}
    {!therapist&&confirmed&&c.status==='COMPLETED'&&<form className="form-grid" onSubmit={e=>{e.preventDefault();act(()=>api(`/experience/circles/${c.id}/feedback`,'PUT',{rating:Number(rating),comment}));}}><h3>{c.feedbackSubmitted?'Update your feedback':'How was your experience?'}</h3><Label>Experience rating<select value={rating} onChange={e=>setRating(e.target.value)}>{[1,2,3,4,5].map(n=><option key={n} value={n}>{n} / 5</option>)}</select></Label><Label>Private feedback<textarea maxLength="2000" value={comment} onChange={e=>setComment(e.target.value)}/></Label><button className="btn" disabled={busy}>Save feedback</button></form>}
  </article>;
}
function PrivacyPanel({api,act,busy}) {
  const [phrase,setPhrase]=useState('');
  return <section className="card"><h2>You decide what happens to your data.</h2><div className="privacy-grid"><article><h3>You</h3><p>Can read and correct your intake, decline invitations, export your records, and withdraw.</p></article><article><h3>Your assigned therapist</h3><p>Can review the summary you confirm and prepare your proposed group. Other therapists cannot access your review queue.</p></article><article><h3>Other participants</h3><p>Cannot view your intake, goals, email or review notes. Video services have their own identity and privacy settings.</p></article></div><p className="muted">This release uses fictional data only. Google-mode processing sends intake to Google services. Redaction reduces some identifiers; it cannot guarantee anonymity. Deleting app records does not override Google’s retention terms or remove provider backups immediately.</p>
    <button className="btn secondary" disabled={busy} onClick={()=>act(async()=>{const d=await api('/experience/export');saveFile('irene-my-data.json',JSON.stringify(d,null,2));return {message:'Your private export has downloaded.'};})}>Download my data</button>
    <div className="destructive-box"><h3>Withdraw and delete intake data</h3><p>This permanently deletes your intake history, AI features, matches, memberships and feedback from the application database. It also withdraws you from circles. Your login and minimal audit records remain.</p><Label>Type DELETE to confirm<input value={phrase} onChange={e=>setPhrase(e.target.value)} autoComplete="off"/></Label><button className="btn danger-btn" disabled={busy||phrase!=='DELETE'} onClick={async()=>{const r=await act(()=>api('/experience/my-intake','DELETE'));if(r)setPhrase('');}}>Delete my intake data</button></div>
  </section>;
}
function CredentialPanel({credentials,admin,api,act,busy}) {
  const [form,setForm]=useState({name:'',qualification:'',reference:'',region:'IN'});
  if(admin)return <>{credentials.filter(c=>c.status==='PENDING').map(c=><CredentialReview key={c.userId} credential={c} api={api} act={act} busy={busy}/>)}{!credentials.some(c=>c.status==='PENDING')&&<Empty>No professional applications awaiting review.</Empty>}<h3>Review history</h3>{credentials.filter(c=>c.status!=='PENDING').map(c=><p key={c.userId}>{c.name} — {c.status.toLowerCase()}</p>)}</>;
  return <section className="card"><h2>Apply for a professional account</h2><p className="muted">Submit fictional credentials for this demo. An administrator must verify qualifications and jurisdiction before granting therapist access. Use a separate account with no participant intake.</p>{credentials.map(c=><div className="notice" key={c.userId}><Badge>{c.status}</Badge><p>{c.note||'Awaiting administrator review.'}</p></div>)}
    <form className="form-grid" onSubmit={e=>{e.preventDefault();act(()=>api('/experience/credentials','POST',form));}}>{[['name','Professional display name'],['qualification','Qualification'],['reference','Registration reference'],['region','Practice region']].map(([key,label])=><Label key={key}>{label}<input required maxLength={key==='qualification'?300:80} value={form[key]} onChange={e=>setForm({...form,[key]:e.target.value})}/></Label>)}<button className="btn" disabled={busy||credentials.some(c=>['PENDING','VERIFIED'].includes(c.status))}>Submit for verification</button></form></section>;
}
function CredentialReview({credential:c,api,act,busy}) {
  const [note,setNote]=useState(''),[checked,setChecked]=useState(false);
  return <article className="card"><h3>{c.name}</h3><p>{c.qualification} · {c.reference} · {c.region}</p><Label>Verification note<textarea value={note} maxLength="500" onChange={e=>setNote(e.target.value)}/></Label><label className="check"><input type="checkbox" checked={checked} onChange={e=>setChecked(e.target.checked)}/>I reviewed credential evidence and practice jurisdiction. Demo records are fictional.</label><div className="button-row">{['VERIFIED','REJECTED'].map(decision=><button key={decision} className="btn secondary" disabled={busy||!note.trim()||(decision==='VERIFIED'&&!checked)} onClick={()=>act(()=>api(`/experience/credentials/${c.userId}`,'PUT',{decision,note,evidenceReviewed:checked}))}>{decision==='VERIFIED'?'Grant therapist access':'Reject application'}</button>)}</div></article>;
}
function ProgramForm({api,act,busy}) {
  const [form,setForm]=useState({title:'',description:'',issueFocus:'stress,anxiety',goalFocus:'confidence,connection',targetSize:6,scheduleWindow:'mon-19:00,wed-19:00',languageCode:'en',region:'IN',targetAgeBand:'18-24',minGroupSharingComfort:'LOW'});
  const labels={title:'Program title',description:'Purpose and format',issueFocus:'Concerns (comma-separated)',goalFocus:'Goals (comma-separated)',targetSize:'Maximum participants (3–15)',scheduleWindow:'Schedule in Asia/Kolkata (day-HH:mm)',languageCode:'Language code',region:'Practice region',targetAgeBand:'Adult age band',minGroupSharingComfort:'Minimum sharing comfort (LOW, MEDIUM, HIGH)'};
  return <section className="card"><h2>Create a therapist-led program</h2><p className="muted">Programs appear in the catalog only for verified therapists. This release uses Asia/Kolkata as the program scheduling time zone.</p><form className="form-grid two-col" onSubmit={e=>{e.preventDefault();act(()=>api('/therapist/templates','POST',{...form,issueFocus:list(form.issueFocus),goalFocus:list(form.goalFocus),scheduleWindow:list(form.scheduleWindow),targetSize:Number(form.targetSize)}),'Program created. It is now available in the catalog.');}}>{Object.entries(form).map(([key,value])=><Label key={key}>{labels[key]}<input required maxLength={key==='description'?2000:150} type={key==='targetSize'?'number':'text'} min={key==='targetSize'?3:undefined} max={key==='targetSize'?15:undefined} value={value} onChange={e=>setForm({...form,[key]:e.target.value})}/></Label>)}<button className="btn" disabled={busy}>Create program</button></form></section>;
}

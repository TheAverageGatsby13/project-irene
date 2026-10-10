export const API = import.meta.env.VITE_API_BASE || '/api/v1';
export async function request(path, token, method = 'GET', data) {
  const res = await fetch(`${API}${path}`, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, ...(data === undefined ? {} : { body: JSON.stringify(data) }) });
  let body;
  try { body = await res.json(); } catch { throw new Error('The service could not respond. Please try again.'); }
  if (!res.ok) throw new Error(body.message || 'This action could not be completed.');
  return body;
}
export const money = minor => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format((minor || 0) / 100);
export const list = text => text.split(',').map(s => s.trim()).filter(Boolean);
export function saveFile(name, text, type = 'application/json') {
  const url = URL.createObjectURL(new Blob([text], { type })); const a = document.createElement('a'); a.href = url; a.download = name; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export function calendar(circle) {
  const date = value => new Date(value).toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z/, 'Z');
  const end = new Date(new Date(circle.startsAt).getTime() + circle.durationMinutes * 60000);
  saveFile('irene-session.ics', ['BEGIN:VCALENDAR','VERSION:2.0','PRODID:-//Irene//Session//EN','BEGIN:VEVENT',`UID:irene-${circle.id}@irene.local`,`DTSTAMP:${date(new Date())}`,`DTSTART:${date(circle.startsAt)}`,`DTEND:${date(end)}`,'SUMMARY:Irene session',`URL:${circle.meetingUrl || ''}`,'END:VEVENT','END:VCALENDAR'].join('\r\n') + '\r\n', 'text/calendar');
}

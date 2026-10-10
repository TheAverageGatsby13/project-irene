// Entry selection controls navigation only. The API remains the role authority.
export function resolveAccess(access, role) {
  if (!['PATIENT', 'THERAPIST', 'ADMIN'].includes(role)) {
    throw new Error('Your account role could not be verified. Please sign in again.');
  }
  if (access === 'admin' && role !== 'ADMIN') {
    throw new Error('This account does not have administrator access. Choose Participant or Therapist to continue.');
  }
  const professionalEntry = access === 'therapist' && role === 'PATIENT';
  return { initialTab: professionalEntry ? 'credentials' : 'overview', professionalEntry };
}

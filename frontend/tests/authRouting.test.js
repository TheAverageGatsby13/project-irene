import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveAccess } from '../src/authRouting.js';

test('unverified professionals enter their application without gaining a role', () => {
  assert.deepEqual(resolveAccess('therapist', 'PATIENT'), { initialTab: 'credentials', professionalEntry: true });
});
test('verified therapists enter their normal workspace', () => {
  assert.deepEqual(resolveAccess('therapist', 'THERAPIST'), { initialTab: 'overview', professionalEntry: false });
});
test('administrator entry rejects participant and therapist accounts', () => {
  for (const role of ['PATIENT', 'THERAPIST']) assert.throws(() => resolveAccess('admin', role), /does not have administrator access/);
});
test('administrator account can enter administration', () => {
  assert.equal(resolveAccess('admin', 'ADMIN').initialTab, 'overview');
});
test('participant entry respects the real account role', () => {
  for (const role of ['PATIENT', 'THERAPIST', 'ADMIN']) {
    assert.deepEqual(resolveAccess('participant', role), { initialTab: 'overview', professionalEntry: false });
  }
});
test('unknown roles fail closed', () => {
  assert.throws(() => resolveAccess('admin', 'SUPERUSER'), /could not be verified/);
});

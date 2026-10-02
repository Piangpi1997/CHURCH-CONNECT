import { after, before, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, doc, getDoc, getDocs, query, setDoc, updateDoc, serverTimestamp, where } from 'firebase/firestore';

const here = path.dirname(fileURLToPath(import.meta.url));
let env;
const projectId = 'demo-church-connect';

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(path.join(here, '../firestore/firestore.rules'), 'utf8') }
  });
});
after(async () => { await env.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users/member-a'), { uid: 'member-a', churchId: 'church-a', role: 'MEMBER', fullName: 'Member A' });
    await setDoc(doc(db, 'users/member-b'), { uid: 'member-b', churchId: 'church-a', role: 'MEMBER', fullName: 'Member B' });
    await setDoc(doc(db, 'users/admin-a'), { uid: 'admin-a', churchId: 'church-a', role: 'CHURCH_ADMIN', fullName: 'Admin A' });
    await setDoc(doc(db, 'users/admin-b'), { uid: 'admin-b', churchId: 'church-b', role: 'CHURCH_ADMIN', fullName: 'Admin B' });
    await setDoc(doc(db, 'users/finance-a'), { uid: 'finance-a', churchId: 'church-a', role: 'FINANCE_ADMIN', fullName: 'Finance A' });
    await setDoc(doc(db, 'registrations/reg-a'), { userId: 'member-a', churchId: 'church-a', status: 'SUBMITTED', applicantName: 'Member A' });
    await setDoc(doc(db, 'registrations/reg-b'), { userId: 'member-b', churchId: 'church-a', status: 'SUBMITTED', applicantName: 'Member B' });
    await setDoc(doc(db, 'payments/pay-a'), { userId: 'member-a', churchId: 'church-a', status: 'PENDING', amount: 2000 });
    await setDoc(doc(db, 'auditLogs/log-a'), { actorUid: 'admin-a', churchId: 'church-a', action: 'CREATED' });
    await setDoc(doc(db, 'notifications/n-a'), { recipientUserId: 'member-a', churchId: 'church-a', title: 'Private', body: 'Only for A', readAt: null });
    await setDoc(doc(db, 'churches/church-a'), { name: 'Church A', currency: 'MYR' });
  });
});

test('member can read own application/payment but not another member private data', async () => {
  const db = env.authenticatedContext('member-a').firestore();
  await assertSucceeds(getDoc(doc(db, 'registrations/reg-a')));
  await assertSucceeds(getDoc(doc(db, 'payments/pay-a')));
  await assertFails(getDoc(doc(db, 'registrations/reg-b')));
  await assertSucceeds(getDocs(query(collection(db, 'registrations'), where('userId', '==', 'member-a'))));
  await assertFails(getDocs(query(collection(db, 'registrations'), where('userId', '==', 'member-b'))));
});

test('cross-church administrator cannot read another church application', async () => {
  const db = env.authenticatedContext('admin-b').firestore();
  await assertFails(getDoc(doc(db, 'registrations/reg-a')));
});

test('finance role can read payments but not member profiles or complete applications', async () => {
  const db = env.authenticatedContext('finance-a').firestore();
  await assertSucceeds(getDoc(doc(db, 'payments/pay-a')));
  await assertFails(getDoc(doc(db, 'registrations/reg-a')));
  await assertFails(getDoc(doc(db, 'users/member-a')));
});

test('client cannot self-approve, change payment state, or elevate role', async () => {
  const db = env.authenticatedContext('member-a').firestore();
  await assertFails(updateDoc(doc(db, 'registrations/reg-a'), { status: 'APPROVED' }));
  await assertFails(updateDoc(doc(db, 'payments/pay-a'), { status: 'PAID' }));
  await assertFails(updateDoc(doc(db, 'users/member-a'), { role: 'CHURCH_ADMIN' }));
});

test('even church administrators must use trusted server workflow for protected writes', async () => {
  const db = env.authenticatedContext('admin-a').firestore();
  await assertFails(updateDoc(doc(db, 'registrations/reg-a'), { status: 'APPROVED' }));
  await assertFails(updateDoc(doc(db, 'auditLogs/log-a'), { action: 'TAMPERED' }));
  await assertFails(setDoc(doc(db, 'auditLogs/fake'), { action: 'FORGED', churchId: 'church-a' }));
});

test('member may mark only their own notification read', async () => {
  const db = env.authenticatedContext('member-a').firestore();
  await assertSucceeds(updateDoc(doc(db, 'notifications/n-a'), { readAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(db, 'notifications/n-a'), { title: 'Changed' }));
  await assertFails(getDoc(doc(db, 'notifications/n-b')));
});

test('public church configuration can be read but never client-written', async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertSucceeds(getDoc(doc(db, 'churches/church-a')));
  await assertFails(setDoc(doc(db, 'churches/church-a'), { name: 'Attacker' }));
});

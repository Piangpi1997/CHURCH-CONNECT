import { after, before, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc } from 'firebase/firestore';
import { ref, uploadBytes, getBytes } from 'firebase/storage';

const here = path.dirname(fileURLToPath(import.meta.url));
let env;
const projectId = 'demo-church-connect';
const png = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0]);

before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(path.join(here, '../firestore/firestore.rules'), 'utf8') },
    storage: { rules: fs.readFileSync(path.join(here, '../firestore/storage.rules'), 'utf8') },
  });
});
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users/member-a'), { uid: 'member-a', churchId: 'church-a', role: 'MEMBER' });
    await setDoc(doc(db, 'users/member-b'), { uid: 'member-b', churchId: 'church-a', role: 'MEMBER' });
    await setDoc(doc(db, 'users/admin-a'), { uid: 'admin-a', churchId: 'church-a', role: 'CHURCH_ADMIN' });
    await setDoc(doc(db, 'users/admin-b'), { uid: 'admin-b', churchId: 'church-b', role: 'CHURCH_ADMIN' });
  });
});

test('member profile image upload is limited to the owner and same-church context', async () => {
  const ownRef = ref(env.authenticatedContext('member-a').storage(), 'churches/church-a/members/member-a/profile/avatar.png');
  await assertSucceeds(uploadBytes(ownRef, png, { contentType: 'image/png' }));
  const ownBytes = await assertSucceeds(getBytes(ownRef));
  assert.equal(new Uint8Array(ownBytes)[0], 137);

  const otherMemberRef = ref(env.authenticatedContext('member-b').storage(), 'churches/church-a/members/member-a/profile/avatar.png');
  await assertFails(getBytes(otherMemberRef));
  await assertFails(uploadBytes(otherMemberRef, png, { contentType: 'image/png' }));
  const otherChurchRef = ref(env.authenticatedContext('admin-b').storage(), 'churches/church-a/members/member-a/profile/private.png');
  await assertFails(getBytes(otherChurchRef));
  await assertFails(uploadBytes(otherChurchRef, png, { contentType: 'image/png' }));
});

test('church assets and documents require trusted same-church staff and valid content', async () => {
  const admin = env.authenticatedContext('admin-a').storage();
  const asset = ref(admin, 'churches/church-a/assets/banner.png');
  await assertSucceeds(uploadBytes(asset, png, { contentType: 'image/png' }));
  await assertSucceeds(getBytes(asset));
  const badAsset = ref(admin, 'churches/church-a/assets/script.txt');
  await assertFails(uploadBytes(badAsset, new Uint8Array([1, 2, 3]), { contentType: 'text/plain' }));

  const memberAsset = ref(env.authenticatedContext('member-a').storage(), 'churches/church-a/assets/member-upload.png');
  await assertFails(uploadBytes(memberAsset, png, { contentType: 'image/png' }));
  const crossChurchAsset = ref(env.authenticatedContext('admin-b').storage(), 'churches/church-a/assets/forged.png');
  await assertFails(uploadBytes(crossChurchAsset, png, { contentType: 'image/png' }));

  const memberDoc = ref(admin, 'churches/church-a/documents/record.pdf');
  await assertSucceeds(uploadBytes(memberDoc, new Uint8Array([37, 80, 68, 70]), { contentType: 'application/pdf' }));
  await assertFails(getBytes(ref(env.authenticatedContext('member-a').storage(), 'churches/church-a/documents/record.pdf')));
  await assertFails(getBytes(ref(env.authenticatedContext('admin-b').storage(), 'churches/church-a/documents/record.pdf')));
});

import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

const uid = process.env.ADMIN_UID;
const churchId = process.env.CHURCH_ID || 'cmf-setapak';
if (process.env.ALLOW_INITIAL_ADMIN_BOOTSTRAP !== 'YES') {
  throw new Error('Set ALLOW_INITIAL_ADMIN_BOOTSTRAP=YES only for the authorized initial setup.');
}
if (!uid || !/^[A-Za-z0-9_-]{8,128}$/.test(uid)) throw new Error('Set ADMIN_UID to the Firebase Auth UID of the designated church administrator.');
initializeApp({ credential: applicationDefault() });
const db = getFirestore();
const userRef = db.collection('users').doc(uid);
const profile = await userRef.get();
if (!profile.exists) throw new Error('The designated person must first create and verify their account in the app.');
if (profile.get('churchId') !== churchId) throw new Error('The user profile belongs to a different church; no role was changed.');
const existing = await db.collection('users').where('churchId', '==', churchId).where('role', 'in', ['SUPER_ADMIN', 'CHURCH_ADMIN']).get();
if (existing.docs.some((doc) => doc.id !== uid)) throw new Error('An administrator already exists. Use the church’s approved privileged-access procedure instead.');
await userRef.update({ role: 'CHURCH_ADMIN', updatedAt: FieldValue.serverTimestamp() });
console.log(`Initial CHURCH_ADMIN role set for UID ${uid} in ${churchId}.`);

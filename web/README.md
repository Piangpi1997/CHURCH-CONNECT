# CMF Church Web — Phase 1 scaffold

Responsive React + Vite church website with public pages, Member Portal and Admin Dashboard **entry screens**. The public pages are static and the private entry screens are deliberately non-functional until a church-owned Firebase project and security verification are supplied.

## Start
```bash
cd web
npm install
npm run dev
npm run build
```

## Boundaries
- Public: Home, About, Ministries, Events, Sermons, Contact.
- Portal: member feature overview; **no login, registration, member data or QR issuance yet**.
- Admin: admin feature overview; **no privileged operations yet**.
- No production Firebase, payment integration or FCM. No sample church service dates are presented as facts.
- The Android app and its backend/security rules remain unchanged.
- Do not enable private features using client-side role checks alone. Implement Firebase Auth, church-scoped server authorization, App Check and audited backend calls before accepting real member data.
- The public contact/address and schedule need church approval.
- Deployment/domain setup is not included. No secrets should be committed.

## Next implementation
Connect verified Firebase environment, build authenticated member/admin routes, reuse server-owned registration/finance/approval workflows, add E2E and accessibility tests, and only then deploy production.

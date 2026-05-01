# Deploy backend on Render

## 1) Push code to GitHub
- Push this repository (including `render.yaml`) to a GitHub repo.

## 2) Create services from Blueprint
- In Render dashboard: **New +** -> **Blueprint**.
- Select your GitHub repository.
- Render will auto-create:
  - Postgres database: `gps-db`
  - Web service: `gps-backend`

## 3) Deploy and get public API URL
- Wait until deploy is green.
- Open web service URL, for example:
  - `https://gps-backend-xxxx.onrender.com`
- Health check: open `/` and verify response.

## 4) Point mobile app to public backend
- Set in `mobile/.env`:
  - `EXPO_PUBLIC_API_URL=https://gps-backend-xxxx.onrender.com`
- Rebuild release APK and share `app-release.apk`.

## 5) Notes
- `JWT_SECRET` is generated automatically by Render.
- Database uses `DATABASE_URL` from Render managed Postgres.
- `DB_SSL=true` is already configured in `render.yaml`.

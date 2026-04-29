# Speed App (Expo + NestJS + PostgreSQL)

Production-ready starter for GPS speed tracking and race leaderboard.

## Stack

- Mobile: Expo (React Native + TypeScript)
- Backend: NestJS + TypeORM
- Database: PostgreSQL (Docker)

## Run Backend + DB

```bash
docker compose up --build
```

Backend runs at `http://localhost:3000`.
PostgreSQL (DBeaver): host `localhost`, port `5005`, user `postgres`, password `123123`, database `GPS`.

## Run Mobile

```bash
cd mobile
npm start
```

Default API base URL is set for Android emulator: `http://10.0.2.2:3000` in `mobile/src/api.ts`.
For iOS simulator use `http://localhost:3000`.
For real device use your machine LAN IP.

## Implemented APIs

- `POST /auth/register-phone` (body: `phoneNumber`, `password`, `displayName`)
- `POST /auth/login-phone` (body: `phoneNumber`, `password`, returns JWT)
- `POST /users` (multipart: `vehicleName`, `engineType`, optional `avatar`)
- `GET /users/:id`
- `PUT /users/:id` (multipart update)
- `POST /results`
- `GET /results/global?mode=GPS|RACE_201|RACE_402`
- `GET /results/vehicle?vehicleName=Exciter%20155&mode=GPS|RACE_201|RACE_402`

## SQL Schema

- See `schema.sql`.

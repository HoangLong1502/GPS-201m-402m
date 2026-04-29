CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "phoneNumber" varchar(20) UNIQUE,
  "displayName" varchar(80),
  "passwordHash" text,
  avatar text,
  "vehicleName" varchar(80),
  "engineType" varchar(80),
  "createdAt" timestamptz NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'results_mode_enum') THEN
    CREATE TYPE results_mode_enum AS ENUM ('GPS', 'STOPWATCH', 'RACE_201', 'RACE_402');
  END IF;
END$$;

CREATE TABLE IF NOT EXISTS results (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "userId" uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  mode results_mode_enum NOT NULL,
  "maxSpeed" double precision NOT NULL,
  time double precision NOT NULL,
  distance double precision NOT NULL,
  "createdAt" timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_results_mode ON results(mode);
CREATE INDEX IF NOT EXISTS idx_results_user_id ON results("userId");

import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { TypeOrmModuleOptions } from '@nestjs/typeorm';
import { User } from './users/user.entity';
import { Result } from './results/result.entity';
import { UsersModule } from './users/users.module';
import { ResultsModule } from './results/results.module';
import { AuthModule } from './auth/auth.module';

const toBool = (value?: string) => value === 'true' || value === '1';

const getDatabaseConfig = (): TypeOrmModuleOptions => {
  const databaseUrl = process.env.DATABASE_URL;
  const sslEnabled = toBool(process.env.DB_SSL);

  const commonConfig: TypeOrmModuleOptions = {
    type: 'postgres',
    entities: [User, Result],
    synchronize: true,
    ssl: sslEnabled ? { rejectUnauthorized: false } : false,
  };

  if (databaseUrl) {
    return {
      ...commonConfig,
      url: databaseUrl,
    };
  }

  return {
    ...commonConfig,
    host: process.env.DB_HOST ?? 'postgres',
    port: Number(process.env.DB_PORT ?? 5005),
    username: process.env.DB_USERNAME ?? 'postgres',
    password: process.env.DB_PASSWORD ?? '123123',
    database: process.env.DB_NAME ?? 'GPS',
  };
};

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    TypeOrmModule.forRoot(getDatabaseConfig()),
    UsersModule,
    ResultsModule,
    AuthModule,
  ],
})
export class AppModule {}

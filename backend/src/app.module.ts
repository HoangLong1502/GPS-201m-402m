import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { User } from './users/user.entity';
import { Result } from './results/result.entity';
import { UsersModule } from './users/users.module';
import { ResultsModule } from './results/results.module';
import { AuthModule } from './auth/auth.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    TypeOrmModule.forRoot({
      type: 'postgres',
      host: process.env.DB_HOST ?? 'postgres',
      port: Number(process.env.DB_PORT ?? 5005),
      username: process.env.DB_USERNAME ?? 'postgres',
      password: process.env.DB_PASSWORD ?? '123123',
      database: process.env.DB_NAME ?? 'GPS',
      entities: [User, Result],
      synchronize: true,
    }),
    UsersModule,
    ResultsModule,
    AuthModule,
  ],
})
export class AppModule {}

import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ResultsController } from './results.controller';
import { ResultsService } from './results.service';
import { Result } from './result.entity';
import { User } from '../users/user.entity';
import { AuthModule } from '../auth/auth.module';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Module({
  imports: [TypeOrmModule.forFeature([Result, User]), AuthModule],
  controllers: [ResultsController],
  providers: [ResultsService, JwtAuthGuard],
})
export class ResultsModule {}

import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Result } from './result.entity';
import { User } from '../users/user.entity';
import { CreateResultDto } from './dto/create-result.dto';
import { LeaderboardQueryDto } from './dto/leaderboard-query.dto';
import { ResultMode } from './result-mode.enum';

@Injectable()
export class ResultsService {
  constructor(
    @InjectRepository(Result)
    private readonly resultsRepository: Repository<Result>,
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
  ) {}

  async create(userId: string, createResultDto: CreateResultDto) {
    const userExists = await this.usersRepository.exists({ where: { id: userId } });
    if (!userExists) {
      throw new NotFoundException('User not found');
    }
    const result = this.resultsRepository.create({
      ...createResultDto,
      userId,
    });
    return this.resultsRepository.save(result);
  }

  getGlobalLeaderboard(query: LeaderboardQueryDto) {
    return this.buildLeaderboardQuery(query.mode).getMany();
  }

  getVehicleLeaderboard(query: LeaderboardQueryDto) {
    return this.buildLeaderboardQuery(query.mode, query.vehicleName).getMany();
  }

  private buildLeaderboardQuery(mode: ResultMode, vehicleName?: string) {
    const qb = this.resultsRepository
      .createQueryBuilder('result')
      .leftJoinAndSelect('result.user', 'user')
      .where('result.mode = :mode', { mode });

    if (vehicleName) {
      qb.andWhere('user.vehicleName = :vehicleName', { vehicleName });
    }

    if (mode === ResultMode.GPS) {
      qb.orderBy('result.maxSpeed', 'DESC');
    } else {
      qb.orderBy('result.time', 'ASC');
    }

    return qb.limit(50);
  }
}

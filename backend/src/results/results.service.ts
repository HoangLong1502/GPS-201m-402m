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

    const mode = createResultDto.mode;

    // BEST-ONLY rules:
    // - GPS: keep maxSpeed DESC
    // - RACE_201 / RACE_402: keep time ASC
    // - STOPWATCH: keep ALL records (do not replace)
    if (mode === ResultMode.STOPWATCH) {
      const created = this.resultsRepository.create({ ...createResultDto, userId });
      return this.resultsRepository.save(created);
    }

    const existing = await this.resultsRepository.findOne({ where: { userId, mode } });
    if (!existing) {
      const created = this.resultsRepository.create({ ...createResultDto, userId });
      return this.resultsRepository.save(created);
    }

    const shouldReplace =
      mode === ResultMode.GPS
        ? createResultDto.maxSpeed > existing.maxSpeed
        : createResultDto.time < existing.time;

    if (!shouldReplace) return existing;

    existing.maxSpeed = createResultDto.maxSpeed;
    existing.time = createResultDto.time;
    existing.distance = createResultDto.distance;
    return this.resultsRepository.save(existing);
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

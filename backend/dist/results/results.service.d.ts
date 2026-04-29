import { Repository } from 'typeorm';
import { Result } from './result.entity';
import { User } from '../users/user.entity';
import { CreateResultDto } from './dto/create-result.dto';
import { LeaderboardQueryDto } from './dto/leaderboard-query.dto';
export declare class ResultsService {
    private readonly resultsRepository;
    private readonly usersRepository;
    constructor(resultsRepository: Repository<Result>, usersRepository: Repository<User>);
    create(userId: string, createResultDto: CreateResultDto): Promise<Result>;
    getGlobalLeaderboard(query: LeaderboardQueryDto): Promise<Result[]>;
    getVehicleLeaderboard(query: LeaderboardQueryDto): Promise<Result[]>;
    private buildLeaderboardQuery;
}

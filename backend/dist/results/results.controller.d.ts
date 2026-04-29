import { CreateResultDto } from './dto/create-result.dto';
import { LeaderboardQueryDto } from './dto/leaderboard-query.dto';
import { ResultsService } from './results.service';
export declare class ResultsController {
    private readonly resultsService;
    constructor(resultsService: ResultsService);
    create(req: {
        authUser: {
            id: string;
        };
    }, createResultDto: CreateResultDto): Promise<import("./result.entity").Result>;
    global(query: LeaderboardQueryDto): Promise<import("./result.entity").Result[]>;
    vehicle(query: LeaderboardQueryDto): Promise<import("./result.entity").Result[]>;
}

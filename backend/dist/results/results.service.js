"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ResultsService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const result_entity_1 = require("./result.entity");
const user_entity_1 = require("../users/user.entity");
const result_mode_enum_1 = require("./result-mode.enum");
let ResultsService = class ResultsService {
    resultsRepository;
    usersRepository;
    constructor(resultsRepository, usersRepository) {
        this.resultsRepository = resultsRepository;
        this.usersRepository = usersRepository;
    }
    async create(userId, createResultDto) {
        const userExists = await this.usersRepository.exists({ where: { id: userId } });
        if (!userExists) {
            throw new common_1.NotFoundException('User not found');
        }
        const result = this.resultsRepository.create({
            ...createResultDto,
            userId,
        });
        return this.resultsRepository.save(result);
    }
    getGlobalLeaderboard(query) {
        return this.buildLeaderboardQuery(query.mode).getMany();
    }
    getVehicleLeaderboard(query) {
        return this.buildLeaderboardQuery(query.mode, query.vehicleName).getMany();
    }
    buildLeaderboardQuery(mode, vehicleName) {
        const qb = this.resultsRepository
            .createQueryBuilder('result')
            .leftJoinAndSelect('result.user', 'user')
            .where('result.mode = :mode', { mode });
        if (vehicleName) {
            qb.andWhere('user.vehicleName = :vehicleName', { vehicleName });
        }
        if (mode === result_mode_enum_1.ResultMode.GPS) {
            qb.orderBy('result.maxSpeed', 'DESC');
        }
        else {
            qb.orderBy('result.time', 'ASC');
        }
        return qb.limit(50);
    }
};
exports.ResultsService = ResultsService;
exports.ResultsService = ResultsService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(result_entity_1.Result)),
    __param(1, (0, typeorm_1.InjectRepository)(user_entity_1.User)),
    __metadata("design:paramtypes", [typeorm_2.Repository,
        typeorm_2.Repository])
], ResultsService);
//# sourceMappingURL=results.service.js.map
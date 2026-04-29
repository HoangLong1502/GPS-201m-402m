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
exports.ResultsController = void 0;
const common_1 = require("@nestjs/common");
const create_result_dto_1 = require("./dto/create-result.dto");
const leaderboard_query_dto_1 = require("./dto/leaderboard-query.dto");
const results_service_1 = require("./results.service");
const jwt_auth_guard_1 = require("../auth/jwt-auth.guard");
let ResultsController = class ResultsController {
    resultsService;
    constructor(resultsService) {
        this.resultsService = resultsService;
    }
    create(req, createResultDto) {
        return this.resultsService.create(req.authUser.id, createResultDto);
    }
    global(query) {
        return this.resultsService.getGlobalLeaderboard(query);
    }
    vehicle(query) {
        return this.resultsService.getVehicleLeaderboard(query);
    }
};
exports.ResultsController = ResultsController;
__decorate([
    (0, common_1.Post)(),
    (0, common_1.UseGuards)(jwt_auth_guard_1.JwtAuthGuard),
    __param(0, (0, common_1.Req)()),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, create_result_dto_1.CreateResultDto]),
    __metadata("design:returntype", void 0)
], ResultsController.prototype, "create", null);
__decorate([
    (0, common_1.Get)('global'),
    __param(0, (0, common_1.Query)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [leaderboard_query_dto_1.LeaderboardQueryDto]),
    __metadata("design:returntype", void 0)
], ResultsController.prototype, "global", null);
__decorate([
    (0, common_1.Get)('vehicle'),
    __param(0, (0, common_1.Query)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [leaderboard_query_dto_1.LeaderboardQueryDto]),
    __metadata("design:returntype", void 0)
], ResultsController.prototype, "vehicle", null);
exports.ResultsController = ResultsController = __decorate([
    (0, common_1.Controller)('results'),
    __metadata("design:paramtypes", [results_service_1.ResultsService])
], ResultsController);
//# sourceMappingURL=results.controller.js.map
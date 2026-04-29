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
exports.AuthService = void 0;
const common_1 = require("@nestjs/common");
const typeorm_1 = require("@nestjs/typeorm");
const typeorm_2 = require("typeorm");
const user_entity_1 = require("../users/user.entity");
const bcryptjs_1 = require("bcryptjs");
const jwt_1 = require("@nestjs/jwt");
let AuthService = class AuthService {
    usersRepository;
    jwtService;
    constructor(usersRepository, jwtService) {
        this.usersRepository = usersRepository;
        this.jwtService = jwtService;
    }
    async registerByPhone(dto) {
        const normalizedPhone = dto.phoneNumber.trim();
        const existing = await this.usersRepository.findOne({
            where: { phoneNumber: normalizedPhone },
        });
        if (existing) {
            throw new common_1.ConflictException('Phone number already registered');
        }
        const user = this.usersRepository.create({
            phoneNumber: normalizedPhone,
            passwordHash: await (0, bcryptjs_1.hash)(dto.password, 12),
            displayName: dto.displayName.trim(),
            vehicleName: null,
            engineType: null,
            avatar: null,
        });
        const saved = await this.usersRepository.save(user);
        return this.buildAuthResponse(saved);
    }
    async loginByPhone(dto) {
        const normalizedPhone = dto.phoneNumber.trim();
        const user = await this.usersRepository
            .createQueryBuilder('user')
            .addSelect('user.passwordHash')
            .where('user.phoneNumber = :phoneNumber', { phoneNumber: normalizedPhone })
            .getOne();
        if (!user) {
            throw new common_1.UnauthorizedException('Invalid credentials');
        }
        if (!user.passwordHash) {
            throw new common_1.UnauthorizedException('Invalid credentials');
        }
        const validPassword = await (0, bcryptjs_1.compare)(dto.password, user.passwordHash);
        if (!validPassword) {
            throw new common_1.UnauthorizedException('Invalid credentials');
        }
        return this.buildAuthResponse(user);
    }
    buildAuthResponse(user) {
        const token = this.jwtService.sign({
            sub: user.id,
            phoneNumber: user.phoneNumber,
        });
        return {
            accessToken: token,
            user: {
                id: user.id,
                phoneNumber: user.phoneNumber,
                displayName: user.displayName,
                avatar: user.avatar,
                vehicleName: user.vehicleName,
                engineType: user.engineType,
                createdAt: user.createdAt,
            },
        };
    }
};
exports.AuthService = AuthService;
exports.AuthService = AuthService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, typeorm_1.InjectRepository)(user_entity_1.User)),
    __metadata("design:paramtypes", [typeorm_2.Repository,
        jwt_1.JwtService])
], AuthService);
//# sourceMappingURL=auth.service.js.map
import { ConflictException, Injectable, UnauthorizedException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from '../users/user.entity';
import { RegisterPhoneDto } from './dto/register-phone.dto';
import { LoginPhoneDto } from './dto/login-phone.dto';
import { hash, compare } from 'bcryptjs';
import { JwtService } from '@nestjs/jwt';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
    private readonly jwtService: JwtService,
  ) {}

  async registerByPhone(dto: RegisterPhoneDto) {
    const normalizedPhone = dto.phoneNumber.trim();
    const existing = await this.usersRepository.findOne({
      where: { phoneNumber: normalizedPhone },
    });
    if (existing) {
      throw new ConflictException('Phone number already registered');
    }
    const user = this.usersRepository.create({
      phoneNumber: normalizedPhone,
      passwordHash: await hash(dto.password, 12),
      displayName: dto.displayName.trim(),
      vehicleName: null,
      engineType: null,
      avatar: null,
    });
    const saved = await this.usersRepository.save(user);
    return this.buildAuthResponse(saved);
  }

  async loginByPhone(dto: LoginPhoneDto) {
    const normalizedPhone = dto.phoneNumber.trim();
    const user = await this.usersRepository
      .createQueryBuilder('user')
      .addSelect('user.passwordHash')
      .where('user.phoneNumber = :phoneNumber', { phoneNumber: normalizedPhone })
      .getOne();

    if (!user) {
      throw new UnauthorizedException('Invalid credentials');
    }
    if (!user.passwordHash) {
      throw new UnauthorizedException('Invalid credentials');
    }
    const validPassword = await compare(dto.password, user.passwordHash);
    if (!validPassword) {
      throw new UnauthorizedException('Invalid credentials');
    }
    return this.buildAuthResponse(user);
  }

  private buildAuthResponse(user: User) {
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
}

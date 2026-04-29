import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from './user.entity';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
  ) {}

  create(createUserDto: CreateUserDto, avatar?: string) {
    const user = this.usersRepository.create({
      ...createUserDto,
      phoneNumber: createUserDto.phoneNumber ?? null,
      displayName: createUserDto.displayName ?? null,
      vehicleName: createUserDto.vehicleName ?? null,
      engineType: createUserDto.engineType ?? null,
      avatar: avatar ?? createUserDto.avatar ?? null,
    });
    return this.usersRepository.save(user);
  }

  async findById(id: string) {
    const user = await this.usersRepository.findOne({ where: { id } });
    if (!user) {
      throw new NotFoundException('User not found');
    }
    return user;
  }

  async update(id: string, updateUserDto: UpdateUserDto, avatar?: string) {
    const user = await this.findById(id);
    user.vehicleName = updateUserDto.vehicleName ?? user.vehicleName;
    user.engineType = updateUserDto.engineType ?? user.engineType;
    user.phoneNumber = updateUserDto.phoneNumber ?? user.phoneNumber;
    user.displayName = updateUserDto.displayName ?? user.displayName;
    if (avatar) {
      user.avatar = avatar;
    }
    return this.usersRepository.save(user);
  }
}

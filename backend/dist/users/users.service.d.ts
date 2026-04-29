import { Repository } from 'typeorm';
import { User } from './user.entity';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
export declare class UsersService {
    private readonly usersRepository;
    constructor(usersRepository: Repository<User>);
    create(createUserDto: CreateUserDto, avatar?: string): Promise<User>;
    findById(id: string): Promise<User>;
    update(id: string, updateUserDto: UpdateUserDto, avatar?: string): Promise<User>;
}

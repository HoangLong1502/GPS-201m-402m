import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
export declare class UsersController {
    private readonly usersService;
    constructor(usersService: UsersService);
    create(createUserDto: CreateUserDto, avatar?: {
        filename: string;
    }): Promise<import("./user.entity").User>;
    findOne(id: string): Promise<import("./user.entity").User>;
    update(id: string, updateUserDto: UpdateUserDto, avatar?: {
        filename: string;
    }): Promise<import("./user.entity").User>;
}

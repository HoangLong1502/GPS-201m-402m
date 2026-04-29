import { Repository } from 'typeorm';
import { User } from '../users/user.entity';
import { RegisterPhoneDto } from './dto/register-phone.dto';
import { LoginPhoneDto } from './dto/login-phone.dto';
import { JwtService } from '@nestjs/jwt';
export declare class AuthService {
    private readonly usersRepository;
    private readonly jwtService;
    constructor(usersRepository: Repository<User>, jwtService: JwtService);
    registerByPhone(dto: RegisterPhoneDto): Promise<{
        accessToken: string;
        user: {
            id: string;
            phoneNumber: string | null;
            displayName: string | null;
            avatar: string | null;
            vehicleName: string | null;
            engineType: string | null;
            createdAt: Date;
        };
    }>;
    loginByPhone(dto: LoginPhoneDto): Promise<{
        accessToken: string;
        user: {
            id: string;
            phoneNumber: string | null;
            displayName: string | null;
            avatar: string | null;
            vehicleName: string | null;
            engineType: string | null;
            createdAt: Date;
        };
    }>;
    private buildAuthResponse;
}

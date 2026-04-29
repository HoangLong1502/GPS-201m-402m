import { AuthService } from './auth.service';
import { LoginPhoneDto } from './dto/login-phone.dto';
import { RegisterPhoneDto } from './dto/register-phone.dto';
export declare class AuthController {
    private readonly authService;
    constructor(authService: AuthService);
    registerPhone(dto: RegisterPhoneDto): Promise<{
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
    loginPhone(dto: LoginPhoneDto): Promise<{
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
}

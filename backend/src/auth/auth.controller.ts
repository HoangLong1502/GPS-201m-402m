import { Body, Controller, Post } from '@nestjs/common';
import { AuthService } from './auth.service';
import { LoginPhoneDto } from './dto/login-phone.dto';
import { RegisterPhoneDto } from './dto/register-phone.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register-phone')
  registerPhone(@Body() dto: RegisterPhoneDto) {
    return this.authService.registerByPhone(dto);
  }

  @Post('login-phone')
  loginPhone(@Body() dto: LoginPhoneDto) {
    return this.authService.loginByPhone(dto);
  }
}

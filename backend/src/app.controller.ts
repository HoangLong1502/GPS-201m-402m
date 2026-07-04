import { Controller, Get } from '@nestjs/common';
import { AppService } from './app.service';

@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  /** Health check for Render (`healthCheckPath: /`). */
  @Get()
  getHello(): { status: string; message: string } {
    return { status: 'ok', message: this.appService.getHello() };
  }

  @Get('health')
  health(): { status: string } {
    return { status: 'ok' };
  }
}

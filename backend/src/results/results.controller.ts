import { Body, Controller, Get, Post, Query, Req, UseGuards } from '@nestjs/common';
import { CreateResultDto } from './dto/create-result.dto';
import { LeaderboardQueryDto } from './dto/leaderboard-query.dto';
import { ResultsService } from './results.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('results')
export class ResultsController {
  constructor(private readonly resultsService: ResultsService) {}

  @Post()
  @UseGuards(JwtAuthGuard)
  create(@Req() req: { authUser: { id: string } }, @Body() createResultDto: CreateResultDto) {
    return this.resultsService.create(req.authUser.id, createResultDto);
  }

  @Get('global')
  global(@Query() query: LeaderboardQueryDto) {
    return this.resultsService.getGlobalLeaderboard(query);
  }

  @Get('vehicle')
  vehicle(@Query() query: LeaderboardQueryDto) {
    return this.resultsService.getVehicleLeaderboard(query);
  }
}

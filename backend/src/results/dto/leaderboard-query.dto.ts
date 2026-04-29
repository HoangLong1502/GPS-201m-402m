import { IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';
import { ResultMode } from '../result-mode.enum';

export class LeaderboardQueryDto {
  @IsEnum(ResultMode)
  mode: ResultMode;

  @IsOptional()
  @IsString()
  @MaxLength(80)
  vehicleName?: string;
}

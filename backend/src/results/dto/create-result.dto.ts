import { IsEnum, IsNumber, Max, Min } from 'class-validator';
import { ResultMode } from '../result-mode.enum';

export class CreateResultDto {
  @IsEnum(ResultMode)
  mode: ResultMode;

  @IsNumber()
  @Min(0)
  @Max(500)
  maxSpeed: number;

  @IsNumber()
  @Min(0)
  time: number;

  @IsNumber()
  @Min(0)
  distance: number;
}

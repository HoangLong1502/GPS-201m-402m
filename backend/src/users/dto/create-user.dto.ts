import { IsOptional, IsString, MaxLength } from 'class-validator';

export class CreateUserDto {
  @IsOptional()
  @IsString()
  @MaxLength(20)
  phoneNumber?: string;

  @IsOptional()
  @IsString()
  @MaxLength(80)
  displayName?: string;

  @IsString()
  @IsOptional()
  @MaxLength(80)
  vehicleName?: string;

  @IsString()
  @IsOptional()
  @MaxLength(80)
  engineType?: string;

  @IsOptional()
  @IsString()
  avatar?: string;
}

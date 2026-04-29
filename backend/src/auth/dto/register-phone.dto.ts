import { IsString, Matches, MaxLength, MinLength } from 'class-validator';

export class RegisterPhoneDto {
  @IsString()
  @MaxLength(20)
  @Matches(/^\+?[0-9]{9,15}$/)
  phoneNumber: string;

  @IsString()
  @MinLength(8)
  @MaxLength(72)
  password: string;

  @IsString()
  @MaxLength(80)
  displayName: string;
}

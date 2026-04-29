import { IsString, Matches, MaxLength } from 'class-validator';

export class LoginPhoneDto {
  @IsString()
  @MaxLength(20)
  @Matches(/^\+?[0-9]{9,15}$/, {
    message: 'phoneNumber must be a valid phone number',
  })
  phoneNumber: string;

  @IsString()
  @MaxLength(72)
  password: string;
}

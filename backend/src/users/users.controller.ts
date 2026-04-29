import {
  Body,
  Controller,
  Get,
  Param,
  ParseUUIDPipe,
  Post,
  Put,
  UploadedFile,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { randomUUID } from 'crypto';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';

const avatarStorage = diskStorage({
  destination: 'uploads',
  filename: (_, file, cb) => {
    cb(null, `${randomUUID()}${extname(file.originalname)}`);
  },
});

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post()
  @UseInterceptors(FileInterceptor('avatar', { storage: avatarStorage }))
  create(
    @Body() createUserDto: CreateUserDto,
    @UploadedFile() avatar?: { filename: string },
  ) {
    return this.usersService.create(
      createUserDto,
      avatar ? `/uploads/${avatar.filename}` : undefined,
    );
  }

  @Get(':id')
  findOne(@Param('id', new ParseUUIDPipe()) id: string) {
    return this.usersService.findById(id);
  }

  @Put(':id')
  @UseInterceptors(FileInterceptor('avatar', { storage: avatarStorage }))
  update(
    @Param('id', new ParseUUIDPipe()) id: string,
    @Body() updateUserDto: UpdateUserDto,
    @UploadedFile() avatar?: { filename: string },
  ) {
    return this.usersService.update(
      id,
      updateUserDto,
      avatar ? `/uploads/${avatar.filename}` : undefined,
    );
  }
}

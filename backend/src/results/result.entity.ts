import {
  Column,
  CreateDateColumn,
  Entity,
  JoinColumn,
  ManyToOne,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { User } from '../users/user.entity';
import { ResultMode } from './result-mode.enum';

@Entity({ name: 'results' })
export class Result {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column('uuid')
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userId' })
  user: User;

  @Column({ type: 'enum', enum: ResultMode })
  mode: ResultMode;

  @Column('float')
  maxSpeed: number;

  @Column('float')
  time: number;

  @Column('float')
  distance: number;

  @CreateDateColumn()
  createdAt: Date;
}

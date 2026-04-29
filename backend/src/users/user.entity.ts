import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn } from 'typeorm';

@Entity({ name: 'users' })
export class User {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'text', nullable: true })
  avatar: string | null;

  @Column({ type: 'varchar', length: 20, unique: true, nullable: true })
  phoneNumber: string | null;

  @Column({ type: 'varchar', length: 80, nullable: true })
  displayName: string | null;

  @Column({ type: 'text', select: false, nullable: true })
  passwordHash: string | null;

  @Column({ type: 'varchar', length: 80, nullable: true })
  vehicleName: string | null;

  @Column({ type: 'varchar', length: 80, nullable: true })
  engineType: string | null;

  @CreateDateColumn()
  createdAt: Date;
}

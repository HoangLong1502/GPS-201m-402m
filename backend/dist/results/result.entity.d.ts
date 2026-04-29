import { User } from '../users/user.entity';
import { ResultMode } from './result-mode.enum';
export declare class Result {
    id: string;
    userId: string;
    user: User;
    mode: ResultMode;
    maxSpeed: number;
    time: number;
    distance: number;
    createdAt: Date;
}

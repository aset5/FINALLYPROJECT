export type Role = 'STUDENT' | 'COMPANY' | 'UNIVERSITY_ADMIN' | 'ADMIN';

export interface User {
  id: number;
  username: string;
  fullName?: string;
  email?: string;
  phone?: string;
  resume?: string;
  resumePath?: string;
  role: Role;
  universityId?: number;
  universityName?: string;
  telegramChatId?: number;
  enabled?: boolean;
}

export interface University {
  id: number;
  name: string;
}

export interface Internship {
  id: number;
  title: string;
  city?: string;
  description?: string;
  studyMaterials?: string;
  status: string;
  maxPlaces: number;
  joinedCount: number;
  universityId?: number;
  universityName?: string;
  companyId?: number;
  companyName?: string;
  companyJob: boolean;
}

export interface Application {
  id: number;
  status: string;
  appliedAt?: string;
  student?: User;
  internship: Internship;
  finalGradePercent?: number;
  gradeLetter?: string;
  completedAt?: string;
}

export interface CompletedProgram {
  applicationId: number;
  certificateNumber?: string;
  programTitle: string;
  universityName?: string;
  finalGradePercent?: number;
  gradeLetter?: string;
  completedAt?: string;
}

export interface StudentPublicProfile {
  user: User;
  completedPrograms: CompletedProgram[];
}

export interface Company {
  id?: number;
  name?: string;
  bin?: string;
  userId?: number;
}

export interface Message {
  id: number;
  content: string;
  sentAt: string;
  senderUsername: string;
  senderId: number;
}

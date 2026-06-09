import type { Application, Internship, Message, User } from '../types';

export interface ProgramLesson {
  id: number;
  internshipId: number;
  sortOrder: number;
  title: string;
  content?: string;
  externalUrl?: string;
  filePath?: string;
  fileName?: string;
  completed: boolean;
  scorePercent?: number | null;
  hasCheckQuestion: boolean;
  checkQuestion?: string | null;
  checkOptions: string[];
  checkCorrectIndex?: number | null;
}

export interface GradeInfo {
  moduleAveragePercent: number;
  finalTestPercent?: number | null;
  overallGradePercent: number;
  gradeLetter: string;
  modulesWeightPercent: number;
  finalTestWeightPercent: number;
  minPassPercent: number;
  gradeRequirementMet: boolean;
}

export interface LessonCompleteResult {
  correct: boolean;
  moduleScore: number;
  message: string;
  detail: LearningDetail;
}

export interface ProgramMaterial {
  id: number;
  internshipId: number;
  sortOrder: number;
  title: string;
  type: 'LINK' | 'FILE';
  url?: string;
  filePath?: string;
  fileName?: string;
}

export interface QuizQuestion {
  id: number;
  sortOrder: number;
  questionText: string;
  options: string[];
  correctIndex?: number | null;
}

export interface LearningDetail {
  application: Application;
  internship: Internship;
  lessons: ProgramLesson[];
  materials: ProgramMaterial[];
  quizQuestions: QuizQuestion[];
  progressPercent: number;
  quizPassed: boolean;
  quizScorePercent?: number | null;
  canComplete: boolean;
  quizPassThreshold: number;
  hasQuiz: boolean;
  universityContact?: User | null;
  grades: GradeInfo;
}

export interface QuizSubmitResult {
  scorePercent: number;
  passed: boolean;
  requiredPercent: number;
  detail: LearningDetail;
}

export interface ProgramContentData {
  internship: Internship;
  lessons: ProgramLesson[];
  materials: ProgramMaterial[];
  quizQuestions: QuizQuestion[];
}

export interface UniversityChatData {
  history: Message[];
  student?: User;
  universityUser?: User;
  internshipId: number;
  internshipTitle?: string;
  currentUsername?: string;
}

export interface ImproveResumeResponse {
  improvedText: string;
  tips: string[];
}

export interface JobMatchItem {
  internshipId: number;
  title: string;
  companyName: string;
  city?: string;
  matchPercent: number;
  summary: string;
  skillsToImprove: string[];
}

export interface JobMatchStats {
  totalJobs: number;
  analyzedJobs: number;
  averageMatchPercent: number;
  highMatchCount: number;
  bestMatchInternshipId: number | null;
  bestMatchPercent: number;
}

export interface JobMatchResponse {
  matches: JobMatchItem[];
  overallAdvice: string;
  stats: JobMatchStats;
}

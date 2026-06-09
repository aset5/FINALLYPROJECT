import { Link } from 'react-router-dom';
import { Badge, Button } from 'react-bootstrap';
import { downloadFile } from '../api/client';
import type { CompletedProgram } from '../types';

export type CertificateDownloadKind = 'student' | 'company' | 'university';

interface Props {
  programs: CompletedProgram[];
  studentId?: number;
  downloadKind?: CertificateDownloadKind;
  compact?: boolean;
}

function formatDate(iso?: string) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  } catch {
    return '—';
  }
}

function certificateUrl(kind: CertificateDownloadKind, applicationId: number, studentId?: number) {
  if (kind === 'student') {
    return `/api/student/learning/${applicationId}/certificate`;
  }
  if (kind === 'company' && studentId != null) {
    return `/api/company/students/${studentId}/certificate/${applicationId}`;
  }
  return `/api/university-admin/applications/${applicationId}/certificate`;
}

export default function StudentAchievementsPanel({
  programs,
  studentId,
  downloadKind = 'student',
  compact = false,
}: Props) {
  if (programs.length === 0) {
    return (
      <div className={`achievements-empty ${compact ? 'achievements-empty--compact' : ''}`}>
        <i className="bi bi-mortarboard" aria-hidden />
        <p className="mb-0">
          {compact
            ? 'Завершённых программ ВУЗа пока нет'
            : 'После завершения программы обучения здесь появятся оценка и сертификат'}
        </p>
      </div>
    );
  }

  return (
    <div className="achievements-list">
      {programs.map((p) => (
        <article key={p.applicationId} className="achievement-card">
          <div className="achievement-card__icon">
            <i className="bi bi-award-fill" aria-hidden />
          </div>
          <div className="achievement-card__body">
            <h6 className="achievement-card__title">{p.programTitle}</h6>
            {p.universityName && (
              <p className="achievement-card__uni mb-1">
                <i className="bi bi-building me-1" />
                {p.universityName}
              </p>
            )}
            <div className="achievement-card__meta">
              {p.finalGradePercent != null ? (
                <Badge className="badge-modern primary me-2">
                  {p.finalGradePercent}%
                  {p.gradeLetter ? ` · ${p.gradeLetter}` : ''}
                </Badge>
              ) : (
                <Badge bg="secondary" className="me-2">
                  Без оценки
                </Badge>
              )}
              <span className="achievement-card__date">
                <i className="bi bi-calendar-check me-1" />
                {formatDate(p.completedAt)}
              </span>
            </div>
          </div>
          <div className="d-flex flex-column gap-1 achievement-card__actions">
            <Button
              size="sm"
              className="btn-gradient achievement-card__cert"
              onClick={() =>
                downloadFile(
                  certificateUrl(downloadKind, p.applicationId, studentId),
                  `certificate-${p.applicationId}.pdf`,
                ).catch((err) => alert(err instanceof Error ? err.message : 'Ошибка скачивания'))
              }
            >
              <i className="bi bi-file-earmark-pdf me-1" />
              PDF
            </Button>
            {p.certificateNumber && (
              <Link
                to={`/verify/${encodeURIComponent(p.certificateNumber)}`}
                className="btn btn-outline-modern btn-sm text-center"
              >
                Проверить
              </Link>
            )}
          </div>
        </article>
      ))}
    </div>
  );
}

import { useEffect, useState } from 'react';
import { Alert, Button, Modal, Spinner } from 'react-bootstrap';
import { api, downloadFile } from '../api/client';
import StudentAchievementsPanel from './StudentAchievementsPanel';
import type { CertificateDownloadKind } from './StudentAchievementsPanel';
import type { StudentPublicProfile } from '../types';

interface Props {
  studentId: number | null;
  onClose: () => void;
  apiBase: '/api/company/students' | '/api/university-admin/students';
  downloadKind: CertificateDownloadKind;
  showResumeDownload?: boolean;
}

function displayFileName(resumePath?: string) {
  if (!resumePath) return '';
  return resumePath.includes('_') ? resumePath.split('_').slice(1).join('_') : resumePath;
}

export default function StudentProfileModal({
  studentId,
  onClose,
  apiBase,
  downloadKind,
  showResumeDownload = false,
}: Props) {
  const [profile, setProfile] = useState<StudentPublicProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (studentId == null) {
      setProfile(null);
      setError('');
      return;
    }
    setLoading(true);
    setError('');
    api
      .get<StudentPublicProfile>(`${apiBase}/${studentId}`)
      .then(setProfile)
      .catch((err) => setError(err instanceof Error ? err.message : 'Не удалось загрузить профиль'))
      .finally(() => setLoading(false));
  }, [studentId, apiBase]);

  const user = profile?.user;
  const hasFile = Boolean(user?.resumePath);
  const hasText = Boolean(user?.resume?.trim());

  return (
    <Modal show={studentId != null} onHide={onClose} centered size="lg" contentClassName="modal-glass">
      <Modal.Header closeButton>
        <Modal.Title>
          <i className="bi bi-person-badge me-2" />
          {user?.fullName || user?.username || 'Профиль студента'}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {loading && (
          <div className="text-center py-5">
            <Spinner animation="border" style={{ color: 'var(--ip-primary)' }} />
          </div>
        )}
        {error && <Alert variant="danger" className="alert-modern">{error}</Alert>}
        {!loading && !error && user && (
          <>
            <div className="student-meta">
              {user.email && (
                <span className="student-meta-item">
                  <i className="bi bi-envelope" />
                  {user.email}
                </span>
              )}
              {user.universityName && (
                <span className="student-meta-item">
                  <i className="bi bi-mortarboard" />
                  {user.universityName}
                </span>
              )}
            </div>

            <div className="achievements-section mb-4">
              <div className="achievements-section__head">
                <div className="achievements-section__icon">
                  <i className="bi bi-trophy-fill" aria-hidden />
                </div>
                <div>
                  <h3 className="achievements-section__title">Завершённые программы</h3>
                  <p className="achievements-section__sub">Оценка и официальный сертификат</p>
                </div>
              </div>
              <StudentAchievementsPanel
                programs={profile?.completedPrograms ?? []}
                studentId={user.id}
                downloadKind={downloadKind}
                compact
              />
            </div>

            <h6 className="fw-bold mb-2">
              <i className="bi bi-card-text me-2 text-primary" />
              О себе
            </h6>
            {hasText ? (
              <div className="resume-text-box mb-4">{user.resume}</div>
            ) : (
              <Alert variant="light" className="alert-modern border mb-4 small">
                Студент не заполнил текстовое резюме.
              </Alert>
            )}

            {hasFile && showResumeDownload ? (
              <div className="resume-file-box">
                <div>
                  <i className="bi bi-file-earmark-pdf fs-4 text-primary d-block mb-1" />
                  <span className="fw-semibold small">{displayFileName(user.resumePath)}</span>
                </div>
                <button
                  type="button"
                  className="btn btn-gradient btn-sm"
                  onClick={() =>
                    downloadFile(
                      `/api/company/download/resume/${user.id}`,
                      displayFileName(user.resumePath) || 'resume',
                    ).catch((err) => alert(err instanceof Error ? err.message : 'Ошибка скачивания'))
                  }
                >
                  <i className="bi bi-download me-1" />
                  Скачать файл
                </button>
              </div>
            ) : hasFile ? (
              <Alert variant="light" className="alert-modern border mb-0 small">
                <i className="bi bi-paperclip me-2" />
                Прикреплён файл резюме
              </Alert>
            ) : (
              <Alert variant="light" className="alert-modern border mb-0 small">
                <i className="bi bi-paperclip me-2" />
                Файл резюме не прикреплён.
              </Alert>
            )}
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="light" onClick={onClose}>
          Закрыть
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

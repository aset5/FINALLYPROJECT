import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Badge, Col, Container, Form, Row } from 'react-bootstrap';
import AiResumeAssistant from '../../components/AiResumeAssistant';
import LoadingScreen from '../../components/LoadingScreen';
import StudentAchievementsPanel from '../../components/StudentAchievementsPanel';
import { api, downloadFile } from '../../api/client';
import type { CompletedProgram, User } from '../../types';

interface StudentProfileData {
  user: User;
  telegramBotUsername: string;
  telegramConnected: boolean;
  telegramConnectUrl: string;
  completedPrograms: CompletedProgram[];
}

export default function ProfilePage() {
  const [data, setData] = useState<StudentProfileData | null>(null);
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [passwords, setPasswords] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [resumeText, setResumeText] = useState('');

  const load = useCallback(() => api.get<StudentProfileData>('/api/student/profile').then(setData), []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (data?.user.resume != null) {
      setResumeText(data.user.resume);
    }
  }, [data?.user.id, data?.user.resume]);

  const onProfileSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    const form = new FormData(e.currentTarget);
    try {
      const updated = await api.putForm<StudentProfileData>('/api/student/profile', form);
      setData(updated);
      setMsg('Профиль успешно обновлён');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка');
    }
  };

  const onPasswordSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await api.post('/api/student/profile/password', passwords);
      setMsg('Пароль изменён');
      setPasswords({ oldPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка');
    }
  };

  if (!data) return <LoadingScreen />;

  const { user, telegramConnected, telegramConnectUrl } = data;
  const resumeUrl = user.resumePath ? `/uploads/resumes/${user.resumePath}` : null;

  return (
    <Container className="py-5">
      <div className="mb-4">
        <Link to="/student/applications" className="text-link small">
          <i className="bi bi-arrow-left me-1" />
          Вернуться к откликам
        </Link>
      </div>

      <Row className="justify-content-center">
        <Col lg={8}>
          <div className="card-modern p-4 p-md-5">
            <div className="d-flex align-items-center mb-4">
              <div
                className="p-3 rounded-circle me-3"
                style={{ background: 'linear-gradient(135deg, #e0e7ff, #c7d2fe)' }}
              >
                <i className="bi bi-person-lines-fill fs-3 text-primary" />
              </div>
              <div>
                <h2 className="fw-bold mb-0">Личный профиль</h2>
                <p className="text-muted mb-0">Резюме и уведомления в Telegram</p>
              </div>
            </div>

            {msg && (
              <Alert variant="success" dismissible onClose={() => setMsg('')}>
                <i className="bi bi-check-circle-fill me-2" />
                {msg}
              </Alert>
            )}
            {error && (
              <Alert variant="danger" dismissible onClose={() => setError('')}>
                {error}
              </Alert>
            )}

            <div className="mb-4">
              <Form.Label className="fw-bold">Уведомления в Telegram</Form.Label>
              {!telegramConnected ? (
                <div className="tg-box-connect d-flex align-items-center justify-content-between flex-wrap gap-3">
                  <div>
                    <i className="bi bi-telegram text-primary me-2" />
                    <span className="small">Подключите бота для мгновенных уведомлений о статусе откликов</span>
                  </div>
                  <div className="d-flex gap-2">
                    <a
                      href={telegramConnectUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-outline-primary btn-sm rounded-pill px-3"
                    >
                      <i className="bi bi-plus-circle me-1" />
                      Подключить
                    </a>
                    <button type="button" className="btn btn-outline-modern btn-sm" onClick={load}>
                      Проверить
                    </button>
                  </div>
                </div>
              ) : (
                <div className="tg-box-connected d-flex align-items-center justify-content-between flex-wrap gap-2">
                  <span className="text-success fw-bold small">
                    <i className="bi bi-patch-check-fill me-2" />
                    Telegram уведомления активны
                  </span>
                  <Badge bg="success">@{data.telegramBotUsername}</Badge>
                </div>
              )}
            </div>

            <div className="achievements-section">
              <div className="achievements-section__head">
                <div className="achievements-section__icon">
                  <i className="bi bi-trophy-fill" aria-hidden />
                </div>
                <div>
                  <h3 className="achievements-section__title">Обучение и сертификаты</h3>
                  <p className="achievements-section__sub">
                    Завершённые программы ВУЗа видят работодатели и кураторы
                  </p>
                </div>
              </div>
              <StudentAchievementsPanel
                programs={data.completedPrograms ?? []}
                downloadKind="student"
              />
            </div>

            <hr className="opacity-10 mb-4" />

            <Form onSubmit={onProfileSubmit}>
              <Form.Group className="mb-4">
                <Form.Label className="fw-bold">Ваш логин</Form.Label>
                <Form.Control value={user.username} readOnly className="bg-light" />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label className="fw-bold">ФИО</Form.Label>
                <Form.Control name="fullName" defaultValue={user.fullName} key={`name-${user.id}`} />
              </Form.Group>

              <Form.Group className="mb-4">
                <Form.Label className="fw-bold">Email</Form.Label>
                <Form.Control
                  name="email"
                  type="email"
                  defaultValue={user.email}
                  key={`email-${user.id}`}
                />
              </Form.Group>

              <Form.Group className="mb-4">
                <Form.Label className="fw-bold">Файл резюме (PDF, DOCX)</Form.Label>
                <div className="upload-zone">
                  <i className="bi bi-cloud-arrow-up fs-2 text-muted d-block mb-2" />
                  <Form.Control
                    type="file"
                    name="resumeFile"
                    accept=".pdf,.doc,.docx"
                    className="mt-2"
                  />
                  <Form.Text className="d-block mt-2">Максимальный размер: 5 MB</Form.Text>
                </div>

                {resumeUrl && (
                  <div className="mt-3 p-3 border rounded bg-light d-flex align-items-center gap-2">
                    <i className="bi bi-file-earmark-check-fill text-success fs-4" />
                    <div className="flex-grow-1">
                      <span className="small fw-bold d-block">Текущий документ:</span>
                      <button
                        type="button"
                        className="btn btn-link btn-sm p-0 small"
                        onClick={() =>
                          downloadFile(resumeUrl, user.resumePath?.split('_').slice(1).join('_') || 'resume')
                        }
                      >
                        {user.resumePath?.includes('_')
                          ? user.resumePath.split('_').slice(1).join('_')
                          : user.resumePath}
                      </button>
                    </div>
                    <Badge bg="success">Загружено</Badge>
                  </div>
                )}
              </Form.Group>

              <Form.Group className="mb-4">
                <Form.Label className="fw-bold">Текст резюме / О себе</Form.Label>
                <AiResumeAssistant resumeText={resumeText} onApply={setResumeText} />
                <Form.Control
                  as="textarea"
                  rows={8}
                  name="resume"
                  placeholder="Опишите ваши навыки и опыт..."
                  value={resumeText}
                  onChange={(e) => setResumeText(e.target.value)}
                />
              </Form.Group>

              <div className="d-grid mb-4">
                <button type="submit" className="btn btn-gradient btn-lg py-2">
                  <i className="bi bi-save me-2" />
                  Сохранить профиль
                </button>
              </div>
            </Form>

            <hr className="opacity-10" />

            <h5 className="mb-3">Сменить пароль</h5>
            <Form onSubmit={onPasswordSubmit}>
              <Row>
                <Col md={4}>
                  <Form.Group className="mb-3">
                    <Form.Label>Текущий пароль</Form.Label>
                    <Form.Control
                      type="password"
                      value={passwords.oldPassword}
                      onChange={(e) => setPasswords({ ...passwords, oldPassword: e.target.value })}
                      required
                    />
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group className="mb-3">
                    <Form.Label>Новый пароль</Form.Label>
                    <Form.Control
                      type="password"
                      value={passwords.newPassword}
                      onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })}
                      required
                    />
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group className="mb-3">
                    <Form.Label>Подтверждение</Form.Label>
                    <Form.Control
                      type="password"
                      value={passwords.confirmPassword}
                      onChange={(e) => setPasswords({ ...passwords, confirmPassword: e.target.value })}
                      required
                    />
                  </Form.Group>
                </Col>
              </Row>
              <button type="submit" className="btn btn-outline-modern">
                Обновить пароль
              </button>
            </Form>
          </div>
        </Col>
      </Row>
    </Container>
  );
}

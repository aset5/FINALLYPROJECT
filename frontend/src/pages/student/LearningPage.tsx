import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Container,
  Form,
  Modal,
  Nav,
  ProgressBar,
  Row,
  Tab,
} from 'react-bootstrap';
import { api, downloadFile } from '../../api/client';
import ChatPanel from '../../components/ChatPanel';
import PageHeader from '../../components/PageHeader';
import type {
  LearningDetail,
  LessonCompleteResult,
  ProgramLesson,
  QuizSubmitResult,
  UniversityChatData,
} from '../../types/learning';
type TabKey = 'modules' | 'materials' | 'quiz' | 'chat' | 'certificate';

export default function LearningPage() {
  const { applicationId } = useParams();
  const [data, setData] = useState<LearningDetail | null>(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [activeTab, setActiveTab] = useState<TabKey>('modules');
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [chat, setChat] = useState<UniversityChatData | null>(null);
  const [chatInput, setChatInput] = useState('');
  const [chatSending, setChatSending] = useState(false);
  const [checkLesson, setCheckLesson] = useState<ProgramLesson | null>(null);
  const [checkAnswer, setCheckAnswer] = useState<number | null>(null);

  const load = useCallback(() => {
    return api
      .get<LearningDetail>(`/api/student/learning/${applicationId}`)
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : 'Ошибка загрузки'));
  }, [applicationId]);

  const loadChat = useCallback(() => {
    return api
      .get<UniversityChatData>(`/api/student/learning/${applicationId}/messages`)
      .then(setChat)
      .catch(() => setChat(null));
  }, [applicationId]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (activeTab === 'chat') loadChat();
  }, [activeTab, loadChat]);

  const startCompleteLesson = (lesson: ProgramLesson) => {
    setError('');
    if (lesson.hasCheckQuestion) {
      setCheckLesson(lesson);
      setCheckAnswer(null);
    } else {
      submitLessonComplete(lesson.id);
    }
  };

  const submitLessonComplete = async (lessonId: number, answerIndex?: number) => {
    try {
      const res = await api.post<LessonCompleteResult>(
        `/api/student/learning/${applicationId}/lessons/${lessonId}/complete`,
        answerIndex != null ? { answerIndex } : undefined,
      );
      setData(res.detail);
      setMsg(res.message);
      setCheckLesson(null);
      setCheckAnswer(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка');
    }
  };

  const submitQuiz = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const payload: Record<string, number> = {};
      Object.entries(answers).forEach(([id, idx]) => {
        payload[id] = idx;
      });
      const res = await api.post<QuizSubmitResult>(
        `/api/student/learning/${applicationId}/quiz/submit`,
        payload,
      );
      setData(res.detail);
      setMsg(
        res.passed
          ? `Тест пройден: ${res.scorePercent}%`
          : `Нужно ${res.requiredPercent}%. Ваш результат: ${res.scorePercent}%`,
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка теста');
    }
  };

  const finishProgram = async () => {
    try {
      await api.post(`/api/student/learning/${applicationId}/complete`);
      await load();
      setMsg('Программа завершена! Сертификат доступен во вкладке «Сертификат».');
      setActiveTab('certificate');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось завершить');
    }
  };

  const sendChat = async (e: FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;
    setChatSending(true);
    try {
      await api.post(`/api/student/learning/${applicationId}/messages`, {
        content: chatInput.trim(),
      });
      setChatInput('');
      await loadChat();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка отправки');
    } finally {
      setChatSending(false);
    }
  };

  const downloadCertificate = () => {
    downloadFile(
      `/api/student/learning/${applicationId}/certificate`,
      `certificate-${applicationId}.pdf`,
    ).catch((e) => setError(e instanceof Error ? e.message : 'Ошибка'));
  };

  const downloadLearningFile = (filePath: string, fileName?: string) => {
    downloadFile(`/api/student/learning/files/${encodeURIComponent(filePath)}`, fileName || 'file');
  };

  if (error && !data) {
    return (
      <Container className="py-4">
        <Alert variant="danger">{error}</Alert>
        <Link to="/student/applications">← К заявкам</Link>
      </Container>
    );
  }

  if (!data) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary" />
      </div>
    );
  }

  const isCompleted =
    data.application.status === 'COMPLETED' || data.application.status === 'VERIFIED';
  const quizSubmitted = data.quizScorePercent != null;
  const legacyMaterials = data.internship.studyMaterials?.trim();
  const lessonsTotal = data.lessons.length;
  const lessonsDone = data.lessons.filter((l) => l.completed).length;
  const allLessonsDone = lessonsTotal > 0 && lessonsDone === lessonsTotal;
  const grades = data.grades ?? {
    moduleAveragePercent: 0,
    finalTestPercent: null,
    overallGradePercent: 0,
    gradeLetter: '—',
    modulesWeightPercent: 40,
    finalTestWeightPercent: 60,
    minPassPercent: 70,
    gradeRequirementMet: false,
  };

  return (
    <Container className="py-4">
      <PageHeader
        title={data.internship.title}
        subtitle={data.internship.universityName || 'Программа обучения'}
        actions={
          <Link to="/student/applications" className="btn btn-outline-modern btn-sm">
            ← Мои отклики
          </Link>
        }
      />

      {msg && (
        <Alert variant="success" className="alert-modern" dismissible onClose={() => setMsg('')}>
          {msg}
        </Alert>
      )}
      {error && (
        <Alert variant="danger" className="alert-modern" dismissible onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Card className="card-modern border-0 mb-4 p-3">
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
          <span className="fw-semibold">Прогресс обучения</span>
          <Badge bg={isCompleted ? 'success' : 'primary'}>{data.progressPercent}%</Badge>
        </div>
        <ProgressBar now={data.progressPercent} variant={isCompleted ? 'success' : 'primary'} className="mb-2" />
        <p className="small text-muted mb-1">
          Модули: <strong>{lessonsDone}</strong> из <strong>{lessonsTotal}</strong>
          {lessonsTotal === 0 && ' — куратор ещё не добавил модули'}
        </p>
        {lessonsTotal > 0 && !allLessonsDone && (
          <p className="small text-warning mb-1">
            <i className="bi bi-info-circle me-1" />
            Завершить программу можно только после прохождения всех модулей
          </p>
        )}
        {data.hasQuiz && (
          <p className="small text-muted mb-2">
            Итоговый тест:{' '}
            {data.quizPassed
              ? `${data.quizScorePercent}%`
              : `нужно ≥ ${data.quizPassThreshold}%${data.quizScorePercent != null ? ` · сейчас ${data.quizScorePercent}%` : ''}`}
          </p>
        )}
        <div className="p-3 rounded" style={{ background: 'var(--ip-bg, #f8fafc)' }}>
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
            <span className="fw-semibold small">Итоговая оценка</span>
            <Badge bg={grades.gradeRequirementMet ? 'success' : 'warning'} className="fs-6">
              {grades.overallGradePercent}% · {grades.gradeLetter}
            </Badge>
          </div>
          <p className="small text-muted mb-1">
            Модули (средний): <strong>{grades.moduleAveragePercent}%</strong>
            {data.hasQuiz && (
              <>
                {' '}
                · вес {grades.modulesWeightPercent}% / {grades.finalTestWeightPercent}%
              </>
            )}
          </p>
          {!grades.gradeRequirementMet && allLessonsDone && (
            <p className="small text-warning mb-0">
              Для завершения нужна итоговая оценка не ниже {grades.minPassPercent}%
            </p>
          )}
        </div>
      </Card>

      <Tab.Container activeKey={activeTab} onSelect={(k) => setActiveTab((k as TabKey) || 'modules')}>
        <Nav variant="pills" className="flex-wrap gap-1 mb-4">
          <Nav.Item>
            <Nav.Link eventKey="modules">Модули</Nav.Link>
          </Nav.Item>
          <Nav.Item>
            <Nav.Link eventKey="materials">Материалы</Nav.Link>
          </Nav.Item>
          {data.hasQuiz && (
            <Nav.Item>
              <Nav.Link eventKey="quiz">Тест</Nav.Link>
            </Nav.Item>
          )}
          <Nav.Item>
            <Nav.Link eventKey="chat">Чат с ВУЗом</Nav.Link>
          </Nav.Item>
          <Nav.Item>
            <Nav.Link eventKey="certificate">Сертификат</Nav.Link>
          </Nav.Item>
        </Nav>

        <Tab.Content>
          <Tab.Pane eventKey="modules">
            {data.lessons.length === 0 ? (
              <Card className="card-modern border-0 p-4">
                {legacyMaterials ? (
                  <pre className="mb-0" style={{ whiteSpace: 'pre-wrap' }}>
                    {legacyMaterials}
                  </pre>
                ) : (
                  <p className="text-muted mb-0">Модули пока не добавлены куратором.</p>
                )}
              </Card>
            ) : (
              <Row className="g-3">
                {data.lessons.map((lesson, i) => (
                  <Col md={6} key={lesson.id}>
                    <Card className={`card-modern border-0 h-100 ${lesson.completed ? 'border-success' : ''}`}>
                      <Card.Body>
                        <div className="d-flex justify-content-between align-items-start mb-2">
                          <Badge bg="secondary">Модуль {i + 1}</Badge>
                          {lesson.completed && (
                            <Badge bg="success">
                              <i className="bi bi-check-lg" /> {lesson.scorePercent}%
                            </Badge>
                          )}
                        </div>
                        <h5 className="fw-bold">{lesson.title}</h5>
                        {lesson.content && (
                          <p className="text-muted small" style={{ whiteSpace: 'pre-wrap' }}>
                            {lesson.content}
                          </p>
                        )}
                        {lesson.externalUrl && (
                          <a href={lesson.externalUrl} target="_blank" rel="noreferrer" className="text-link small d-block mb-2">
                            Открыть ссылку
                          </a>
                        )}
                        {lesson.filePath && (
                          <Button
                            size="sm"
                            variant="outline-secondary"
                            className="mb-2"
                            onClick={() => downloadLearningFile(lesson.filePath!, lesson.fileName)}
                          >
                            <i className="bi bi-download me-1" />
                            {lesson.fileName || 'Скачать файл'}
                          </Button>
                        )}
                        {!lesson.completed && !isCompleted && (
                          <Button size="sm" className="btn-gradient" onClick={() => startCompleteLesson(lesson)}>
                            {lesson.hasCheckQuestion ? 'Пройти проверку' : 'Завершить модуль'}
                          </Button>
                        )}
                      </Card.Body>
                    </Card>
                  </Col>
                ))}
              </Row>
            )}
          </Tab.Pane>

          <Tab.Pane eventKey="materials">
            {data.materials.length === 0 ? (
              <Card className="card-modern border-0 p-4 text-muted">Дополнительные материалы не добавлены.</Card>
            ) : (
              <Row className="g-3">
                {data.materials.map((m) => (
                  <Col md={6} key={m.id}>
                    <Card className="card-modern border-0 h-100">
                      <Card.Body>
                        <h6 className="fw-bold">{m.title}</h6>
                        {m.type === 'LINK' && m.url && (
                          <a href={m.url} target="_blank" rel="noreferrer" className="text-link">
                            Открыть
                          </a>
                        )}
                        {m.type === 'FILE' && m.filePath && (
                          <Button
                            size="sm"
                            variant="outline-modern"
                            onClick={() => downloadLearningFile(m.filePath!, m.fileName)}
                          >
                            Скачать {m.fileName || 'файл'}
                          </Button>
                        )}
                      </Card.Body>
                    </Card>
                  </Col>
                ))}
              </Row>
            )}
          </Tab.Pane>

          {data.hasQuiz && (
            <Tab.Pane eventKey="quiz">
              <Card className="card-modern border-0 p-4">
                {quizSubmitted && (
                  <Alert
                    variant={data.quizPassed ? 'success' : 'warning'}
                    className="alert-modern mb-4"
                  >
                    {data.quizPassed ? (
                      <>
                        Тест пройден: {data.quizScorePercent}%. Повторная сдача недоступна.
                      </>
                    ) : (
                      <>
                        Тест сдан: {data.quizScorePercent}% (нужно ≥ {data.quizPassThreshold}%).
                        Повторная попытка невозможна — варианты ответов заблокированы.
                      </>
                    )}
                  </Alert>
                )}
                {!quizSubmitted && !isCompleted && (
                  <p className="small text-muted mb-3">
                    Одна попытка: после отправки изменить ответы будет нельзя.
                  </p>
                )}
                <Form onSubmit={submitQuiz}>
                  {data.quizQuestions.map((q, qi) => (
                    <div key={q.id} className="mb-4">
                      <p className="fw-semibold mb-2">
                        {qi + 1}. {q.questionText}
                      </p>
                      {q.options.map((opt, oi) => (
                        <Form.Check
                          key={oi}
                          type="radio"
                          name={`q-${q.id}`}
                          id={`q-${q.id}-${oi}`}
                          label={opt}
                          checked={answers[q.id] === oi}
                          onChange={() => setAnswers((prev) => ({ ...prev, [q.id]: oi }))}
                          disabled={isCompleted || quizSubmitted}
                          className="mb-1"
                        />
                      ))}
                    </div>
                  ))}
                  {!isCompleted && !quizSubmitted && (
                    <Button type="submit" className="btn-gradient">
                      Отправить ответы (одна попытка)
                    </Button>
                  )}
                </Form>
              </Card>
            </Tab.Pane>
          )}

          <Tab.Pane eventKey="chat">
            {!data.universityContact ? (
              <Card className="card-modern border-0 p-4">
                <Alert variant="warning" className="mb-0 alert-modern">
                  <i className="bi bi-exclamation-triangle me-2" />
                  Куратор ВУЗа не назначен — чат недоступен.
                </Alert>
              </Card>
            ) : (
              <ChatPanel
                messages={chat?.history ?? []}
                currentUsername={chat?.currentUsername}
                peerName={
                  data.universityContact.fullName?.trim() ||
                  data.universityContact.username ||
                  'Куратор ВУЗа'
                }
                peerSubtitle="Вопросы по программе обучения"
                peerKind="university"
                inputValue={chatInput}
                onInputChange={setChatInput}
                onSubmit={sendChat}
                placeholder="Ваш вопрос куратору..."
                emptyHint="Спросите о модулях, дедлайнах или оценке — куратор ответит в этом чате"
                sending={chatSending}
              />
            )}
          </Tab.Pane>

          <Tab.Pane eventKey="certificate">
            <Card className="card-modern border-0 p-4 text-center">
              {isCompleted ? (
                <>
                  <i className="bi bi-award display-4 text-warning mb-3" />
                  <h5>Поздравляем!</h5>
                  <p className="text-muted">Вы завершили программу. Скачайте сертификат в PDF.</p>
                  <Button className="btn-gradient" onClick={downloadCertificate}>
                    <i className="bi bi-download me-2" />
                    Скачать сертификат
                  </Button>
                </>
              ) : (
                <>
                  <p className="text-muted">
                    Сертификат будет доступен после прохождения всех модулей
                    {data.hasQuiz ? ' и успешного теста' : ''}.
                  </p>
                  {data.canComplete && (
                    <Button className="btn-gradient mt-2" onClick={finishProgram}>
                      Завершить программу
                    </Button>
                  )}
                </>
              )}
            </Card>
          </Tab.Pane>
        </Tab.Content>
      </Tab.Container>

      {!isCompleted && data.canComplete && activeTab !== 'certificate' && (
        <div className="text-center mt-4">
          <Button className="btn-gradient px-4" onClick={finishProgram}>
            Завершить программу ({grades.overallGradePercent}%)
          </Button>
        </div>
      )}

      <Modal show={checkLesson != null} onHide={() => setCheckLesson(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Контрольный вопрос</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {checkLesson && (
            <>
              <p className="fw-semibold mb-3">{checkLesson.checkQuestion}</p>
              {(checkLesson.checkOptions ?? []).map((opt, oi) => (
                <Form.Check
                  key={oi}
                  type="radio"
                  name="module-check"
                  id={`check-${checkLesson.id}-${oi}`}
                  label={opt}
                  checked={checkAnswer === oi}
                  onChange={() => setCheckAnswer(oi)}
                  className="mb-2"
                />
              ))}
              <p className="small text-muted mt-2 mb-0">
                Верный ответ — 100% за модуль, неверный — 50% (снижает итоговую оценку).
              </p>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setCheckLesson(null)}>
            Отмена
          </Button>
          <Button
            className="btn-gradient"
            disabled={checkAnswer == null || !checkLesson}
            onClick={() => checkLesson && submitLessonComplete(checkLesson.id, checkAnswer!)}
          >
            Отправить ответ
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}

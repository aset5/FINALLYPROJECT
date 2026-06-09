import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Alert, Button, Card, Col, Form, Modal, Row } from 'react-bootstrap';
import { api } from '../../api/client';
import type { ProgramContentData, ProgramLesson, ProgramMaterial, QuizQuestion } from '../../types/learning';
import { internshipStatusLabel, internshipStatusVariant } from '../../utils/internshipStatus';

interface Props {
  internshipId: number | null;
  internshipTitle?: string;
  onClose: () => void;
  onProgramUpdated?: () => void;
}

const emptyLessonForm = () => ({
  title: '',
  content: '',
  externalUrl: '',
  checkQuestion: '',
  checkOptions: ['', '', '', ''],
  checkCorrectIndex: 0,
});

const emptyQuizForm = () => ({
  questionText: '',
  options: ['', '', '', ''],
  correctIndex: 0,
});

export default function ProgramContentPanel({
  internshipId,
  internshipTitle,
  onClose,
  onProgramUpdated,
}: Props) {
  const [data, setData] = useState<ProgramContentData | null>(null);
  const [error, setError] = useState('');
  const [moderationNote, setModerationNote] = useState('');
  const [lessonForm, setLessonForm] = useState(emptyLessonForm);
  const [materialForm, setMaterialForm] = useState({ title: '', url: '' });
  const [quizForm, setQuizForm] = useState(emptyQuizForm);
  const [editingLesson, setEditingLesson] = useState<ProgramLesson | null>(null);
  const [editingMaterial, setEditingMaterial] = useState<ProgramMaterial | null>(null);
  const [editingQuiz, setEditingQuiz] = useState<QuizQuestion | null>(null);

  const load = useCallback(() => {
    if (internshipId == null) return;
    api
      .get<ProgramContentData>(`/api/university-admin/learning/internships/${internshipId}`)
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : 'Ошибка'));
  }, [internshipId]);

  useEffect(() => {
    load();
    setModerationNote('');
  }, [load]);

  const afterContentChange = (message?: string) => {
    if (message) setModerationNote(message);
    load();
    onProgramUpdated?.();
  };

  const addLesson = async (e: FormEvent) => {
    e.preventDefault();
    if (!internshipId) return;
    const payload: Record<string, unknown> = {
      title: lessonForm.title,
      content: lessonForm.content,
      externalUrl: lessonForm.externalUrl || null,
    };
    if (lessonForm.checkQuestion.trim()) {
      payload.checkQuestion = lessonForm.checkQuestion.trim();
      payload.checkOptions = lessonForm.checkOptions;
      payload.checkCorrectIndex = lessonForm.checkCorrectIndex;
    }
    await api.post(`/api/university-admin/learning/internships/${internshipId}/lessons`, payload);
    setLessonForm(emptyLessonForm());
    afterContentChange('Изменения отправлены на повторную модерацию.');
  };

  const saveLesson = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingLesson) return;
    const payload: Record<string, unknown> = {
      title: lessonForm.title,
      content: lessonForm.content,
      externalUrl: lessonForm.externalUrl || null,
      checkQuestion: lessonForm.checkQuestion.trim() || null,
    };
    if (lessonForm.checkQuestion.trim()) {
      payload.checkOptions = lessonForm.checkOptions;
      payload.checkCorrectIndex = lessonForm.checkCorrectIndex;
    }
    await api.put(`/api/university-admin/learning/lessons/${editingLesson.id}`, payload);
    setEditingLesson(null);
    setLessonForm(emptyLessonForm());
    afterContentChange('Модуль обновлён. Программа снова на модерации.');
  };

  const openLessonEdit = (lesson: ProgramLesson) => {
    setEditingLesson(lesson);
    setLessonForm({
      title: lesson.title,
      content: lesson.content || '',
      externalUrl: lesson.externalUrl || '',
      checkQuestion: lesson.checkQuestion || '',
      checkOptions:
        lesson.checkOptions?.length === 4 ? [...lesson.checkOptions] : ['', '', '', ''],
      checkCorrectIndex: lesson.checkCorrectIndex ?? 0,
    });
  };

  const addMaterial = async (e: FormEvent) => {
    e.preventDefault();
    if (!internshipId) return;
    await api.post(`/api/university-admin/learning/internships/${internshipId}/materials`, {
      title: materialForm.title,
      type: 'LINK',
      url: materialForm.url,
    });
    setMaterialForm({ title: '', url: '' });
    afterContentChange('Материал добавлен. Программа снова на модерации.');
  };

  const saveMaterial = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingMaterial) return;
    await api.put(`/api/university-admin/learning/materials/${editingMaterial.id}`, materialForm);
    setEditingMaterial(null);
    setMaterialForm({ title: '', url: '' });
    afterContentChange('Материал обновлён. Программа снова на модерации.');
  };

  const addQuiz = async (e: FormEvent) => {
    e.preventDefault();
    if (!internshipId) return;
    await api.post(`/api/university-admin/learning/internships/${internshipId}/quiz`, quizForm);
    setQuizForm(emptyQuizForm());
    afterContentChange('Вопрос добавлен. Программа снова на модерации.');
  };

  const saveQuiz = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingQuiz) return;
    await api.put(`/api/university-admin/learning/quiz/${editingQuiz.id}`, quizForm);
    setEditingQuiz(null);
    setQuizForm(emptyQuizForm());
    afterContentChange('Вопрос обновлён. Программа снова на модерации.');
  };

  const openQuizEdit = (q: QuizQuestion) => {
    setEditingQuiz(q);
    setQuizForm({
      questionText: q.questionText,
      options: q.options?.length === 4 ? [...q.options] : ['', '', '', ''],
      correctIndex: q.correctIndex ?? 0,
    });
  };

  const uploadLessonFile = async (lessonId: number, file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    await api.postForm(`/api/university-admin/learning/lessons/${lessonId}/file`, fd);
    afterContentChange('Файл загружен. Программа снова на модерации.');
  };

  const uploadMaterialFile = async (materialId: number, file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    await api.postForm(`/api/university-admin/learning/materials/${materialId}/file`, fd);
    afterContentChange('Файл загружен. Программа снова на модерации.');
  };

  const deleteLesson = async (id: number) => {
    await api.delete(`/api/university-admin/learning/lessons/${id}`);
    afterContentChange('Модуль удалён. Программа снова на модерации.');
  };

  const deleteMaterial = async (id: number) => {
    await api.delete(`/api/university-admin/learning/materials/${id}`);
    afterContentChange('Материал удалён. Программа снова на модерации.');
  };

  const deleteQuiz = async (id: number) => {
    await api.delete(`/api/university-admin/learning/quiz/${id}`);
    afterContentChange('Вопрос удалён. Программа снова на модерации.');
  };

  const renderLessonFields = (onSubmit: (e: FormEvent) => void, submitLabel: string) => (
    <Form onSubmit={onSubmit} className="mb-3">
      <Form.Control
        className="mb-2"
        placeholder="Название модуля"
        value={lessonForm.title}
        onChange={(e) => setLessonForm({ ...lessonForm, title: e.target.value })}
        required
      />
      <Form.Control
        as="textarea"
        className="mb-2"
        placeholder="Текст урока"
        value={lessonForm.content}
        onChange={(e) => setLessonForm({ ...lessonForm, content: e.target.value })}
      />
      <Form.Control
        className="mb-2"
        placeholder="Ссылка (YouTube, статья...)"
        value={lessonForm.externalUrl}
        onChange={(e) => setLessonForm({ ...lessonForm, externalUrl: e.target.value })}
      />
      <Form.Control
        className="mb-2"
        placeholder="Контрольный вопрос (необязательно)"
        value={lessonForm.checkQuestion}
        onChange={(e) => setLessonForm({ ...lessonForm, checkQuestion: e.target.value })}
      />
      {lessonForm.checkQuestion.trim() &&
        lessonForm.checkOptions.map((opt, i) => (
          <Form.Control
            key={i}
            className="mb-2"
            placeholder={`Вариант ${String.fromCharCode(65 + i)}`}
            value={opt}
            onChange={(e) => {
              const checkOptions = [...lessonForm.checkOptions];
              checkOptions[i] = e.target.value;
              setLessonForm({ ...lessonForm, checkOptions });
            }}
            required
          />
        ))}
      {lessonForm.checkQuestion.trim() && (
        <Form.Select
          className="mb-2"
          value={lessonForm.checkCorrectIndex}
          onChange={(e) =>
            setLessonForm({ ...lessonForm, checkCorrectIndex: Number(e.target.value) })
          }
        >
          <option value={0}>Верный: A</option>
          <option value={1}>Верный: B</option>
          <option value={2}>Верный: C</option>
          <option value={3}>Верный: D</option>
        </Form.Select>
      )}
      <Button type="submit" size="sm" className="w-100">
        {submitLabel}
      </Button>
    </Form>
  );

  const renderQuizFields = (onSubmit: (e: FormEvent) => void, submitLabel: string) => (
    <Form onSubmit={onSubmit}>
      <Form.Control
        as="textarea"
        className="mb-2"
        placeholder="Вопрос"
        value={quizForm.questionText}
        onChange={(e) => setQuizForm({ ...quizForm, questionText: e.target.value })}
        required
      />
      {quizForm.options.map((opt, i) => (
        <Form.Control
          key={i}
          className="mb-2"
          placeholder={`Вариант ${String.fromCharCode(65 + i)}`}
          value={opt}
          onChange={(e) => {
            const options = [...quizForm.options];
            options[i] = e.target.value;
            setQuizForm({ ...quizForm, options });
          }}
          required
        />
      ))}
      <Form.Select
        className="mb-2"
        value={quizForm.correctIndex}
        onChange={(e) => setQuizForm({ ...quizForm, correctIndex: Number(e.target.value) })}
      >
        <option value={0}>Правильный: A</option>
        <option value={1}>Правильный: B</option>
        <option value={2}>Правильный: C</option>
        <option value={3}>Правильный: D</option>
      </Form.Select>
      <Button type="submit" size="sm" className="w-100 mb-3">
        {submitLabel}
      </Button>
    </Form>
  );

  return (
    <>
      <Modal show={internshipId != null} onHide={onClose} size="xl" centered scrollable>
        <Modal.Header closeButton>
          <Modal.Title>
            Контент: {internshipTitle}
            {data?.internship && (
              <span className={`badge bg-${internshipStatusVariant(data.internship.status)} ms-2 fs-6`}>
                {internshipStatusLabel(data.internship.status)}
              </span>
            )}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Alert variant="info" className="small py-2">
            Любое изменение модулей, материалов или теста отправляет программу на повторную модерацию.
          </Alert>
          {moderationNote && <Alert variant="warning">{moderationNote}</Alert>}
          {error && <Alert variant="danger">{error}</Alert>}
          {!data ? (
            <div className="text-center py-4">
              <div className="spinner-border" />
            </div>
          ) : (
            <Row className="g-4">
              <Col lg={6}>
                <Card className="border-0 shadow-sm">
                  <Card.Body>
                    <h6 className="fw-bold">Модули</h6>
                    {renderLessonFields(addLesson, 'Добавить модуль')}
                    {data.lessons.map((l) => (
                      <div key={l.id} className="border-bottom py-2">
                        <div className="d-flex justify-content-between align-items-start gap-2">
                          <div>
                            <strong>
                              {l.title}
                              {l.hasCheckQuestion && (
                                <span className="badge bg-info-subtle text-info ms-2 small">с оценкой</span>
                              )}
                            </strong>
                            {l.content && (
                              <p className="small text-muted mb-1 mt-1">
                                {l.content.length > 120 ? `${l.content.slice(0, 120)}…` : l.content}
                              </p>
                            )}
                          </div>
                          <div className="d-flex gap-1 flex-shrink-0">
                            <Button size="sm" variant="outline-primary" onClick={() => openLessonEdit(l)}>
                              Изм.
                            </Button>
                            <Button size="sm" variant="outline-danger" onClick={() => deleteLesson(l.id)}>
                              ×
                            </Button>
                          </div>
                        </div>
                        <Form.Label className="small text-muted mt-1">Файл к модулю</Form.Label>
                        <Form.Control
                          type="file"
                          size="sm"
                          onChange={(e) => {
                            const f = (e.target as HTMLInputElement).files?.[0];
                            if (f) uploadLessonFile(l.id, f);
                          }}
                        />
                      </div>
                    ))}
                  </Card.Body>
                </Card>
              </Col>
              <Col lg={6}>
                <Card className="border-0 shadow-sm mb-3">
                  <Card.Body>
                    <h6 className="fw-bold">Материалы (ссылки)</h6>
                    <Form onSubmit={addMaterial} className="mb-3">
                      <Form.Control
                        className="mb-2"
                        placeholder="Название"
                        value={materialForm.title}
                        onChange={(e) => setMaterialForm({ ...materialForm, title: e.target.value })}
                        required
                      />
                      <Form.Control
                        className="mb-2"
                        placeholder="URL"
                        value={materialForm.url}
                        onChange={(e) => setMaterialForm({ ...materialForm, url: e.target.value })}
                        required
                      />
                      <Button type="submit" size="sm" className="w-100">
                        Добавить ссылку
                      </Button>
                    </Form>
                    {data.materials.map((m) => (
                      <div
                        key={m.id}
                        className="d-flex justify-content-between align-items-center py-1 border-bottom gap-2"
                      >
                        <span className="small">
                          {m.title} {m.url && `· ${m.url}`}
                        </span>
                        <div className="d-flex gap-1 flex-shrink-0">
                          <Button
                            size="sm"
                            variant="outline-primary"
                            onClick={() => {
                              setEditingMaterial(m);
                              setMaterialForm({ title: m.title, url: m.url || '' });
                            }}
                          >
                            Изм.
                          </Button>
                          <Form.Control
                            type="file"
                            size="sm"
                            style={{ maxWidth: 120 }}
                            onChange={(e) => {
                              const f = (e.target as HTMLInputElement).files?.[0];
                              if (f) uploadMaterialFile(m.id, f);
                            }}
                          />
                          <Button size="sm" variant="outline-danger" onClick={() => deleteMaterial(m.id)}>
                            ×
                          </Button>
                        </div>
                      </div>
                    ))}
                  </Card.Body>
                </Card>
                <Card className="border-0 shadow-sm">
                  <Card.Body>
                    <h6 className="fw-bold">Тест</h6>
                    {renderQuizFields(addQuiz, 'Добавить вопрос')}
                    {data.quizQuestions.map((q: QuizQuestion) => (
                      <div key={q.id} className="small border-bottom py-2 d-flex justify-content-between gap-2">
                        <span>
                          {q.questionText} (верный: {String.fromCharCode(65 + (q.correctIndex ?? 0))})
                        </span>
                        <div className="d-flex gap-1 flex-shrink-0">
                          <Button size="sm" variant="outline-primary" onClick={() => openQuizEdit(q)}>
                            Изм.
                          </Button>
                          <Button size="sm" variant="outline-danger" onClick={() => deleteQuiz(q.id)}>
                            ×
                          </Button>
                        </div>
                      </div>
                    ))}
                  </Card.Body>
                </Card>
              </Col>
            </Row>
          )}
        </Modal.Body>
      </Modal>

      <Modal show={editingLesson != null} onHide={() => setEditingLesson(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Редактировать модуль</Modal.Title>
        </Modal.Header>
        <Modal.Body>{renderLessonFields(saveLesson, 'Сохранить модуль')}</Modal.Body>
      </Modal>

      <Modal show={editingMaterial != null} onHide={() => setEditingMaterial(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Редактировать материал</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form
            onSubmit={saveMaterial}
            onReset={() => {
              setEditingMaterial(null);
              setMaterialForm({ title: '', url: '' });
            }}
          >
            <Form.Control
              className="mb-2"
              placeholder="Название"
              value={materialForm.title}
              onChange={(e) => setMaterialForm({ ...materialForm, title: e.target.value })}
              required
            />
            <Form.Control
              className="mb-2"
              placeholder="URL"
              value={materialForm.url}
              onChange={(e) => setMaterialForm({ ...materialForm, url: e.target.value })}
              required
            />
            <Button type="submit" size="sm" className="w-100">
              Сохранить
            </Button>
          </Form>
        </Modal.Body>
      </Modal>

      <Modal show={editingQuiz != null} onHide={() => setEditingQuiz(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Редактировать вопрос</Modal.Title>
        </Modal.Header>
        <Modal.Body>{renderQuizFields(saveQuiz, 'Сохранить вопрос')}</Modal.Body>
      </Modal>
    </>
  );
}

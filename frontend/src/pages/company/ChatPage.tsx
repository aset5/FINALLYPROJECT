import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Alert, Container } from 'react-bootstrap';
import { api } from '../../api/client';
import ChatPanel from '../../components/ChatPanel';
import PageHeader from '../../components/PageHeader';
import { useAuth } from '../../context/AuthContext';
import type { Message, User } from '../../types';

export default function CompanyChatPage() {
  const { internshipId, studentId } = useParams();
  const { user } = useAuth();
  const [history, setHistory] = useState<Message[]>([]);
  const [student, setStudent] = useState<User | null>(null);
  const [content, setContent] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  const load = () =>
    api
      .get<{ history: Message[]; student: User }>(
        `/api/company/messages/${internshipId}/${studentId}`,
      )
      .then((d) => {
        setHistory(d.history);
        setStudent(d.student);
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Ошибка загрузки'));

  useEffect(() => {
    load();
  }, [internshipId, studentId]);

  const send = async (e: FormEvent) => {
    e.preventDefault();
    if (!student || !content.trim()) return;
    setSending(true);
    setError('');
    try {
      await api.post('/api/company/messages', {
        internshipId: Number(internshipId),
        receiverId: student.id,
        content: content.trim(),
      });
      setContent('');
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось отправить');
    } finally {
      setSending(false);
    }
  };

  const peerName = student?.fullName?.trim() || student?.username || 'Студент';

  return (
    <Container className="py-4 chat-page-wrap">
      <p className="mb-2">
        <Link to="/company" className="text-link small">
          <i className="bi bi-arrow-left me-1" />
          В кабинет
        </Link>
      </p>
      <PageHeader title="Чат со студентом" subtitle={peerName} />
      {error && (
        <Alert variant="danger" className="alert-modern mb-3" onClose={() => setError('')} dismissible>
          {error}
        </Alert>
      )}
      <ChatPanel
        messages={history}
        currentUserId={user?.id}
        peerName={peerName}
        peerSubtitle={student?.email || 'Кандидат на стажировку'}
        peerKind="student"
        inputValue={content}
        onInputChange={setContent}
        onSubmit={send}
        placeholder="Ответ студенту..."
        emptyHint="Напишите студенту о статусе заявки или следующих шагах"
        sending={sending}
        disabled={!student}
      />
      <p className="text-center mt-3 mb-0">
        <Link to="/company" className="text-link small">
          <i className="bi bi-arrow-left me-1" />
          В кабинет компании
        </Link>
      </p>
    </Container>
  );
}

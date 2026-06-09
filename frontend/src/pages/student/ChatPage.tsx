import { useEffect, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Alert, Container } from 'react-bootstrap';
import { api } from '../../api/client';
import ChatPanel from '../../components/ChatPanel';
import PageHeader from '../../components/PageHeader';
import type { Message, User } from '../../types';

export default function ChatPage() {
  const { internshipId } = useParams();
  const [history, setHistory] = useState<Message[]>([]);
  const [companyUser, setCompanyUser] = useState<User | null>(null);
  const [currentUsername, setCurrentUsername] = useState('');
  const [content, setContent] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  const load = () =>
    api
      .get<{
        history: Message[];
        companyUser: User;
        currentUsername: string;
      }>(`/api/student/messages/${internshipId}`)
      .then((d) => {
        setHistory(d.history);
        setCompanyUser(d.companyUser);
        setCurrentUsername(d.currentUsername);
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Ошибка загрузки'));

  useEffect(() => {
    load();
  }, [internshipId]);

  const send = async (e: FormEvent) => {
    e.preventDefault();
    if (!companyUser || !content.trim()) return;
    setSending(true);
    setError('');
    try {
      await api.post('/api/student/messages', {
        internshipId: Number(internshipId),
        receiverId: companyUser.id,
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

  const peerName =
    companyUser?.fullName?.trim() || companyUser?.username || 'Компания';

  return (
    <Container className="py-4 chat-page-wrap">
      <p className="mb-2">
        <Link to="/student/applications" className="text-link small">
          <i className="bi bi-arrow-left me-1" />
          К заявкам
        </Link>
      </p>
      <PageHeader title="Чат с компанией" subtitle="Переписка по вакансии и стажировке" />
      {error && (
        <Alert variant="danger" className="alert-modern mb-3" onClose={() => setError('')} dismissible>
          {error}
        </Alert>
      )}
      <ChatPanel
        messages={history}
        currentUsername={currentUsername}
        peerName={peerName}
        peerSubtitle="Представитель работодателя"
        peerKind="company"
        inputValue={content}
        onInputChange={setContent}
        onSubmit={send}
        placeholder="Сообщение компании..."
        emptyHint="Задайте вопрос о вакансии, этапах отбора или условиях стажировки"
        sending={sending}
        disabled={!companyUser}
      />
      <p className="text-center mt-3 mb-0">
        <Link to="/student/applications" className="text-link small">
          <i className="bi bi-arrow-left me-1" />
          Вернуться к заявкам
        </Link>
      </p>
    </Container>
  );
}

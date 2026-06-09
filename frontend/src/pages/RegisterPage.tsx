import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Alert, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { api } from '../api/client';
import type { University } from '../types';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [universities, setUniversities] = useState<University[]>([]);
  const [roleType, setRoleType] = useState('STUDENT');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [registrationDone, setRegistrationDone] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const [form, setForm] = useState({
    username: '',
    password: '',
    fullName: '',
    email: '',
    verificationCode: '',
    universityId: '',
    uniName: '',
  });

  useEffect(() => {
    api.get<University[]>('/api/auth/universities').then(setUniversities);
  }, []);

  const sendCode = async () => {
    setError('');
    setInfo('');
    if (!form.email.trim()) {
      setError('Введите email');
      return;
    }
    setSendingCode(true);
    setEmailVerified(false);
    try {
      const res = await api.post<{ message: string }>('/api/auth/send-verification-code', {
        email: form.email.trim(),
      });
      setInfo(res.message);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка отправки');
    } finally {
      setSendingCode(false);
    }
  };

  const verifyCode = async () => {
    setError('');
    setInfo('');
    if (!form.email.trim() || !form.verificationCode.trim()) {
      setError('Введите email и код из письма');
      return;
    }
    setVerifyingCode(true);
    try {
      await api.post('/api/auth/verify-email-code', {
        email: form.email.trim(),
        code: form.verificationCode.trim(),
      });
      setEmailVerified(true);
      setInfo('Email подтверждён. Заполните остальные поля и завершите регистрацию.');
    } catch (err) {
      setEmailVerified(false);
      setError(err instanceof Error ? err.message : 'Неверный код');
    } finally {
      setVerifyingCode(false);
    }
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!emailVerified) {
      setError('Сначала подтвердите email кодом из письма');
      return;
    }
    try {
      const res = await api.post<{ message: string; pendingApproval?: boolean }>('/api/auth/register', {
        username: form.username,
        password: form.password,
        roleType,
        fullName: form.fullName,
        email: form.email.trim(),
        verificationCode: form.verificationCode.trim(),
        universityId: form.universityId ? Number(form.universityId) : null,
        uniName: form.uniName || null,
      });
      if (res.pendingApproval) {
        setRegistrationDone(true);
        setInfo(res.message);
        return;
      }
      navigate('/login', { state: { message: res.message } });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка регистрации');
    }
  };

  return (
    <div className="auth-page">
      <Container>
        <Row className="justify-content-center">
          <Col md={7} lg={6}>
            <div className="auth-card">
              <div className="auth-card-header">
                <h2>Регистрация</h2>
                <p>Подтвердите email — мы отправим код на почту</p>
              </div>
              <div className="auth-card-body">
                {error && <Alert variant="danger" className="alert-modern">{error}</Alert>}
                {info && (
                  <Alert variant={registrationDone ? 'success' : 'info'} className="alert-modern">
                    {info}
                  </Alert>
                )}
                {registrationDone && (
                  <p className="text-center small mb-3">
                    <Link to="/login" className="text-link">
                      Перейти на страницу входа
                    </Link>
                  </p>
                )}
                {emailVerified && !registrationDone && (
                  <Alert variant="success" className="alert-modern py-2">
                    <i className="bi bi-check-circle-fill me-2" />
                    Email подтверждён
                  </Alert>
                )}

                <Form onSubmit={onSubmit} className={registrationDone ? 'd-none' : undefined}>
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold small">Email *</Form.Label>
                    <div className="d-flex gap-2 flex-wrap">
                      <Form.Control
                        type="email"
                        value={form.email}
                        onChange={(e) => {
                          setForm({ ...form, email: e.target.value });
                          setEmailVerified(false);
                        }}
                        placeholder="name@example.com"
                        required
                        className="flex-grow-1"
                      />
                      <button
                        type="button"
                        className="btn btn-outline-modern"
                        onClick={sendCode}
                        disabled={sendingCode}
                      >
                        {sendingCode ? <Spinner size="sm" /> : 'Отправить код'}
                      </button>
                    </div>
                  </Form.Group>

                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold small">Код из письма (6 цифр) *</Form.Label>
                    <div className="d-flex gap-2">
                      <Form.Control
                        value={form.verificationCode}
                        onChange={(e) => {
                          setForm({ ...form, verificationCode: e.target.value.replace(/\D/g, '').slice(0, 6) });
                          setEmailVerified(false);
                        }}
                        placeholder="123456"
                        maxLength={6}
                        required
                        className="flex-grow-1"
                        style={{ letterSpacing: '0.3em', fontWeight: 600 }}
                      />
                      <button
                        type="button"
                        className="btn btn-gradient"
                        onClick={verifyCode}
                        disabled={verifyingCode || form.verificationCode.length !== 6}
                      >
                        {verifyingCode ? <Spinner size="sm" /> : 'Проверить'}
                      </button>
                    </div>
                    <Form.Text className="text-muted">
                      Если SMTP не настроен, код будет в консоли сервера Spring Boot
                    </Form.Text>
                  </Form.Group>

                  <hr className="opacity-10 my-4" />

                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold small">Тип аккаунта</Form.Label>
                    <Form.Select value={roleType} onChange={(e) => setRoleType(e.target.value)}>
                      <option value="STUDENT">Студент</option>
                      <option value="UNIVERSITY">Представитель ВУЗа</option>
                      <option value="COMPANY">Компания</option>
                    </Form.Select>
                    {(roleType === 'UNIVERSITY' || roleType === 'COMPANY') && (
                      <Form.Text className="text-muted">
                        После регистрации аккаунт будет проверен администратором — вход станет доступен после
                        активации.
                      </Form.Text>
                    )}
                  </Form.Group>

                  {roleType === 'STUDENT' && (
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold small">Университет</Form.Label>
                      <Form.Select
                        value={form.universityId}
                        onChange={(e) => setForm({ ...form, universityId: e.target.value })}
                      >
                        <option value="">— Выберите —</option>
                        {universities.map((u) => (
                          <option key={u.id} value={u.id}>
                            {u.name}
                          </option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  )}

                  {roleType === 'UNIVERSITY' && (
                    <Form.Group className="mb-3">
                      <Form.Label className="fw-semibold small">Название университета</Form.Label>
                      <Form.Control
                        value={form.uniName}
                        onChange={(e) => setForm({ ...form, uniName: e.target.value })}
                        required
                      />
                    </Form.Group>
                  )}

                  <Row>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-semibold small">Логин</Form.Label>
                        <Form.Control
                          value={form.username}
                          onChange={(e) => setForm({ ...form, username: e.target.value })}
                          required
                        />
                      </Form.Group>
                    </Col>
                    <Col md={6}>
                      <Form.Group className="mb-3">
                        <Form.Label className="fw-semibold small">Пароль</Form.Label>
                        <Form.Control
                          type="password"
                          value={form.password}
                          onChange={(e) => setForm({ ...form, password: e.target.value })}
                          required
                        />
                      </Form.Group>
                    </Col>
                  </Row>
                  <Form.Group className="mb-4">
                    <Form.Label className="fw-semibold small">ФИО</Form.Label>
                    <Form.Control
                      value={form.fullName}
                      onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                    />
                  </Form.Group>
                  <button
                    type="submit"
                    className="btn btn-gradient w-100 mb-3 py-2"
                    disabled={!emailVerified}
                  >
                    Создать аккаунт
                  </button>
                  <p className="text-center small mb-0 text-muted">
                    Уже есть аккаунт? <Link to="/login" className="text-link">Войти</Link>
                  </p>
                </Form>
              </div>
            </div>
          </Col>
        </Row>
      </Container>
    </div>
  );
}

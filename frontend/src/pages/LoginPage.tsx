import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Col, Container, Form, Row } from 'react-bootstrap';
import { fetchMe, login as apiLogin } from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

const roleHome: Record<Role, string> = {
  STUDENT: '/',
  COMPANY: '/company/dashboard',
  UNIVERSITY_ADMIN: '/university-admin/dashboard',
  ADMIN: '/admin/dashboard',
};

export default function LoginPage() {
  const { refresh } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const successMessage = (location.state as { message?: string } | null)?.message;
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await apiLogin(username, password);
      await refresh();
      const me = await fetchMe();
      navigate(roleHome[me.role] || '/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Неверный логин или пароль');
    }
  };

  return (
    <div className="auth-page">
      <Container>
        <Row className="justify-content-center">
          <Col md={5} lg={4}>
            <div className="auth-card">
              <div className="auth-card-header">
                <h2>Добро пожаловать</h2>
                <p>Войдите в аккаунт INTERN.PRO</p>
              </div>
              <div className="auth-card-body">
                {successMessage && (
                  <Alert variant="success" className="alert-modern">
                    {successMessage}
                  </Alert>
                )}
                {error && <Alert variant="danger" className="alert-modern">{error}</Alert>}
                <Form onSubmit={onSubmit}>
                  <Form.Group className="mb-3">
                    <Form.Label className="fw-semibold small">Логин</Form.Label>
                    <Form.Control
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="username"
                      required
                    />
                  </Form.Group>
                  <Form.Group className="mb-4">
                    <Form.Label className="fw-semibold small">Пароль</Form.Label>
                    <Form.Control
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      required
                    />
                  </Form.Group>
                  <button type="submit" className="btn btn-gradient w-100 mb-3 py-2">
                    Войти
                  </button>
                  <p className="text-center small mb-0 text-muted">
                    Нет аккаунта?{' '}
                    <Link to="/register" className="text-link">
                      Зарегистрироваться
                    </Link>
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

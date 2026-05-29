import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Alert, Badge, Button, Card, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { api } from '../api/client';

export interface CertificateVerification {
  valid: boolean;
  certNumber: string;
  studentName?: string;
  programTitle?: string;
  universityName?: string;
  finalGradePercent?: number;
  gradeLetter?: string;
  issuedAt?: string;
  message?: string;
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

export default function VerifyCertificatePage() {
  const { certNumber: certFromPath } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const initial =
    certFromPath?.trim() ||
    searchParams.get('number')?.trim() ||
    '';

  const [input, setInput] = useState(initial);
  const [result, setResult] = useState<CertificateVerification | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const verify = useCallback(async (raw: string, updateUrl = true) => {
    const number = raw.trim().toUpperCase();
    if (!number) return;
    setLoading(true);
    setError('');
    setResult(null);
    try {
      const data = await api.get<CertificateVerification>(
        `/api/certificates/verify/${encodeURIComponent(number)}`,
      );
      setResult(data);
      if (updateUrl) {
        navigate(`/verify/${encodeURIComponent(number)}`, { replace: true });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ошибка проверки');
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    if (!initial) return;
    setInput(initial);
    verify(initial, false);
  }, [initial, verify]);

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    verify(input);
  };

  return (
    <Container className="py-5" style={{ maxWidth: 640 }}>
      <div className="text-center mb-4">
        <div
          className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3"
          style={{
            width: 72,
            height: 72,
            background: 'linear-gradient(135deg, #e0e7ff, #c7d2fe)',
          }}
        >
          <i className="bi bi-patch-check-fill fs-2 text-primary" />
        </div>
        <h1 className="fw-bold mb-2">Проверка сертификата</h1>
        <p className="text-muted mb-0">
          Введите регистрационный номер с PDF или отсканируйте QR-код
        </p>
        <p className="text-muted small mt-2 mb-0">
          Чтобы QR открывался с телефона, заходите на сайт по IP в Wi‑Fi (например{' '}
          <code className="user-select-all">http://192.168.1.5:8080</code>), а не через localhost.
        </p>
      </div>

      <Card className="card-modern border-0 p-4 mb-4">
        <Form onSubmit={onSubmit}>
          <Form.Group className="mb-3">
            <Form.Label className="fw-semibold small text-muted text-uppercase">
              Номер сертификата
            </Form.Label>
            <Form.Control
              size="lg"
              value={input}
              onChange={(e) => setInput(e.target.value.toUpperCase())}
              placeholder="IPRO-2026-K7M9P2XQ4R1N"
              className="font-monospace"
              spellCheck={false}
            />
            <Form.Text>Формат: IPRO-ГОД-ТОКЕН (12 символов)</Form.Text>
          </Form.Group>
          <Button type="submit" className="btn-gradient w-100" disabled={loading || !input.trim()}>
            {loading ? (
              <>
                <Spinner size="sm" className="me-2" />
                Проверяем...
              </>
            ) : (
              <>
                <i className="bi bi-search me-2" />
                Проверить
              </>
            )}
          </Button>
        </Form>
      </Card>

      {error && (
        <Alert variant="danger" className="alert-modern">
          {error}
        </Alert>
      )}

      {result && !loading && (
        <Card
          className={`card-modern border-0 p-4 ${result.valid ? 'verify-result--ok' : 'verify-result--fail'}`}
        >
          <div className="text-center mb-3">
            <i
              className={`bi ${result.valid ? 'bi-shield-fill-check text-success' : 'bi-shield-fill-x text-danger'} display-4`}
            />
            <h4 className="fw-bold mt-2 mb-1">
              {result.valid ? 'Сертификат подлинный' : 'Не подтверждён'}
            </h4>
            <p className="text-muted small mb-0">{result.message}</p>
          </div>

          <div className="text-center mb-3">
            <Badge bg={result.valid ? 'success' : 'secondary'} className="font-monospace px-3 py-2">
              {result.certNumber}
            </Badge>
          </div>

          {result.valid && (
            <Row className="g-3 verify-details">
              <Col xs={12}>
                <div className="verify-detail-row">
                  <span className="verify-detail-label">Обучающийся</span>
                  <strong>{result.studentName}</strong>
                </div>
              </Col>
              <Col xs={12}>
                <div className="verify-detail-row">
                  <span className="verify-detail-label">Программа</span>
                  <strong>{result.programTitle}</strong>
                </div>
              </Col>
              {result.universityName && (
                <Col xs={12}>
                  <div className="verify-detail-row">
                    <span className="verify-detail-label">ВУЗ</span>
                    <strong>{result.universityName}</strong>
                  </div>
                </Col>
              )}
              <Col sm={6}>
                <div className="verify-detail-row">
                  <span className="verify-detail-label">Оценка</span>
                  <strong>
                    {result.finalGradePercent != null ? `${result.finalGradePercent}%` : '—'}
                    {result.gradeLetter ? ` · ${result.gradeLetter}` : ''}
                  </strong>
                </div>
              </Col>
              <Col sm={6}>
                <div className="verify-detail-row">
                  <span className="verify-detail-label">Дата выдачи</span>
                  <strong>{formatDate(result.issuedAt)}</strong>
                </div>
              </Col>
            </Row>
          )}
        </Card>
      )}

      <p className="text-center mt-4 mb-0">
        <Link to="/" className="text-link small">
          <i className="bi bi-arrow-left me-1" />
          На главную
        </Link>
      </p>
    </Container>
  );
}

import { useState } from 'react';
import { Alert, Modal, Spinner } from 'react-bootstrap';
import { api } from '../api/client';
import type { ImproveResumeResponse } from '../types/ai';

interface Props {
  resumeText: string;
  onApply: (text: string) => void;
}

export default function AiResumeAssistant({ resumeText, onApply }: Props) {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ImproveResumeResponse | null>(null);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);

  const improve = async () => {
    setLoading(true);
    setError('');
    setResult(null);
    setShowModal(true);
    try {
      const data = await api.post<ImproveResumeResponse>('/api/student/ai/improve-resume', {
        resume: resumeText,
      });
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка AI');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="ai-panel mb-4">
        <div className="d-flex align-items-start gap-3 flex-wrap">
          <div className="ai-icon-wrap">
            <i className="bi bi-stars" />
          </div>
          <div className="flex-grow-1">
            <h6 className="fw-bold mb-1">AI-помощник резюме</h6>
            <p className="small text-muted mb-2 mb-md-0">
              Улучшит формулировки, структуру и подскажет, что добавить
            </p>
          </div>
          <button
            type="button"
            className="btn btn-gradient btn-sm"
            disabled={!resumeText?.trim() || loading}
            onClick={improve}
          >
            {loading ? (
              <>
                <Spinner size="sm" className="me-2" />
                Анализ...
              </>
            ) : (
              <>
                <i className="bi bi-magic me-1" />
                Улучшить с AI
              </>
            )}
          </button>
        </div>
      </div>

      <Modal show={showModal} onHide={() => setShowModal(false)} centered size="lg" contentClassName="modal-glass">
        <Modal.Header closeButton>
          <Modal.Title>
            <i className="bi bi-stars me-2" />
            Улучшенное резюме
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {loading && (
            <div className="text-center py-5">
              <Spinner animation="border" style={{ color: 'var(--ip-primary)' }} />
              <p className="text-muted small mt-3 mb-0">AI анализирует текст...</p>
            </div>
          )}
          {error && <Alert variant="danger" className="alert-modern">{error}</Alert>}
          {result && !loading && (
            <>
              <h6 className="fw-bold">Рекомендации</h6>
              <ul className="small text-muted mb-3">
                {result.tips.map((tip) => (
                  <li key={tip}>{tip}</li>
                ))}
              </ul>
              <h6 className="fw-bold">Улучшенный текст</h6>
              <div className="resume-text-box">{result.improvedText}</div>
            </>
          )}
        </Modal.Body>
        {result && !loading && (
          <Modal.Footer>
            <button type="button" className="btn btn-light" onClick={() => setShowModal(false)}>
              Закрыть
            </button>
            <button
              type="button"
              className="btn btn-gradient"
              onClick={() => {
                onApply(result.improvedText);
                setShowModal(false);
              }}
            >
              Применить к резюме
            </button>
          </Modal.Footer>
        )}
      </Modal>
    </>
  );
}

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Col, Container, Row, Spinner } from 'react-bootstrap';
import LoadingScreen from '../../components/LoadingScreen';
import PageHeader from '../../components/PageHeader';
import { api } from '../../api/client';
import type { Internship } from '../../types';
import type { JobMatchResponse } from '../../types/ai';

function matchColor(percent: number) {
  if (percent >= 80) return 'success';
  if (percent >= 60) return 'primary';
  if (percent >= 40) return 'warning';
  return 'secondary';
}

export default function JobMarketPage() {
  const [jobs, setJobs] = useState<Internship[]>([]);
  const [aiData, setAiData] = useState<JobMatchResponse | null>(null);
  const [loadingJobs, setLoadingJobs] = useState(true);
  const [loadingAi, setLoadingAi] = useState(false);
  const [msg, setMsg] = useState('');
  const [aiError, setAiError] = useState('');

  useEffect(() => {
    api
      .get<{ jobs: Internship[] }>('/api/student/job-market')
      .then((d) => setJobs(d.jobs))
      .finally(() => setLoadingJobs(false));
  }, []);

  const runAiAnalysis = async () => {
    setLoadingAi(true);
    setAiError('');
    try {
      const data = await api.post<JobMatchResponse>('/api/student/ai/job-matches');
      setAiData(data);
    } catch (err) {
      setAiError(err instanceof Error ? err.message : 'Ошибка AI-анализа');
    } finally {
      setLoadingAi(false);
    }
  };

  const apply = async (id: number) => {
    try {
      await api.post(`/api/student/apply/${id}`);
      setMsg('Отклик отправлен');
    } catch (err) {
      setMsg(err instanceof Error ? err.message : 'Ошибка');
    }
  };

  const matchByJobId = (id: number) => aiData?.matches.find((m) => m.internshipId === id);

  const displayJobs = aiData
    ? [...jobs].sort(
        (a, b) =>
          (matchByJobId(b.id)?.matchPercent ?? 0) - (matchByJobId(a.id)?.matchPercent ?? 0),
      )
    : jobs;

  if (loadingJobs) return <LoadingScreen />;

  return (
    <Container className="py-4">
      <PageHeader
        title="Рынок вакансий"
        subtitle="AI поможет найти позиции, которые лучше всего подходят вашему резюме"
        actions={
          <button
            type="button"
            className="btn btn-gradient btn-sm"
            onClick={runAiAnalysis}
            disabled={loadingAi || jobs.length === 0}
          >
            {loadingAi ? (
              <>
                <Spinner size="sm" className="me-2" />
                Анализ...
              </>
            ) : (
              <>
                <i className="bi bi-stars me-1" />
                AI-подбор
              </>
            )}
          </button>
        }
      />

      {msg && (
        <Alert variant="success" className="alert-modern mb-4" dismissible onClose={() => setMsg('')}>
          {msg}
        </Alert>
      )}
      {aiError && (
        <Alert variant="danger" className="alert-modern mb-4">
          {aiError}
          {aiError.includes('резюме') && (
            <>
              {' '}
              <Link to="/student/profile" className="alert-link">
                Перейти в профиль
              </Link>
            </>
          )}
        </Alert>
      )}

      {aiData && (
        <>
          <Row className="g-3 mb-4">
            <Col sm={6} md={3}>
              <div className="stat-card">
                <span className="stat-label">Вакансий</span>
                <strong className="stat-value">{aiData.stats.totalJobs}</strong>
              </div>
            </Col>
            <Col sm={6} md={3}>
              <div className="stat-card">
                <span className="stat-label">Средний match</span>
                <strong className="stat-value">{aiData.stats.averageMatchPercent}%</strong>
              </div>
            </Col>
            <Col sm={6} md={3}>
              <div className="stat-card stat-card-success">
                <span className="stat-label">Высокий match (70%+)</span>
                <strong className="stat-value">{aiData.stats.highMatchCount}</strong>
              </div>
            </Col>
            <Col sm={6} md={3}>
              <div className="stat-card stat-card-accent">
                <span className="stat-label">Лучший match</span>
                <strong className="stat-value">{aiData.stats.bestMatchPercent}%</strong>
              </div>
            </Col>
          </Row>

          <div className="ai-advice-box mb-4">
            <i className="bi bi-lightbulb me-2" />
            <span>{aiData.overallAdvice}</span>
          </div>
        </>
      )}

      {!aiData && jobs.length > 0 && (
        <div className="ai-panel mb-4 text-center py-3">
          <p className="small text-muted mb-2">
            Нажмите «AI-подбор», чтобы увидеть процент совпадения с каждой вакансией
          </p>
        </div>
      )}

      {jobs.length === 0 ? (
        <div className="empty-state card-modern p-4">
          <i className="bi bi-briefcase d-block" />
          Нет доступных вакансий
        </div>
      ) : (
        <Row className="g-4">
          {displayJobs.map((job) => {
            const match = matchByJobId(job.id);
            const percent = match?.matchPercent;

            return (
              <Col md={6} lg={4} key={job.id}>
                <div className={`card card-job accent h-100 ${match && percent! >= 70 ? 'card-job-hot' : ''}`}>
                  <div className="card-body">
                    {percent != null && (
                      <div className="mb-3">
                        <div className="d-flex justify-content-between align-items-center mb-1">
                          <span className="small fw-semibold text-muted">Совпадение с резюме</span>
                          <span className={`fw-bold text-${matchColor(percent)}`}>{percent}%</span>
                        </div>
                        <div className="ai-match-bar">
                          <div
                            className={`ai-match-fill bg-${matchColor(percent)}`}
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                      </div>
                    )}
                    <h6 className="card-title fw-bold">{job.title}</h6>
                    <p className="small text-muted mb-2">
                      {job.companyName}
                      {job.city && ` · ${job.city}`}
                    </p>
                    {match?.summary && (
                      <p className="small mb-2" style={{ lineHeight: 1.5 }}>
                        <i className="bi bi-robot text-primary me-1" />
                        {match.summary}
                      </p>
                    )}
                    {match?.skillsToImprove && match.skillsToImprove.length > 0 && (
                      <div className="mb-3">
                        {match.skillsToImprove.map((s) => (
                          <span key={s} className="badge bg-light text-dark border me-1 mb-1">
                            + {s}
                          </span>
                        ))}
                      </div>
                    )}
                    <p className="small text-muted mb-3 line-clamp-3">{job.description}</p>
                    <button type="button" className="btn btn-gradient-accent btn-sm w-100" onClick={() => apply(job.id)}>
                      Откликнуться
                    </button>
                  </div>
                </div>
              </Col>
            );
          })}
        </Row>
      )}
    </Container>
  );
}

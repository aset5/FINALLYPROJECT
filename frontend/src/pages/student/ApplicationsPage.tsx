import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Badge, Col, Container, Row } from 'react-bootstrap';
import LoadingScreen from '../../components/LoadingScreen';
import PageHeader from '../../components/PageHeader';
import { api } from '../../api/client';
import type { Application, Internship } from '../../types';

interface AppsData {
  isVerified: boolean;
  hasActiveUniversityProgram: boolean;
  activeUniversityApplicationId: number | null;
  appliedInternshipIds: number[];
  acceptedCompanyInternships: number;
  maxCompanyInternships: number;
  canTakeMoreInternships: boolean;
  applications: Application[];
  companyJobs: Internship[];
}

export default function ApplicationsPage() {
  const [data, setData] = useState<AppsData | null>(null);
  const [msg, setMsg] = useState('');

  const load = () => api.get<AppsData>('/api/student/applications').then(setData);

  useEffect(() => {
    load();
  }, []);

  const apply = async (id: number) => {
    try {
      await api.post(`/api/student/apply/${id}`);
      setMsg('Заявка отправлена');
      load();
    } catch (err) {
      setMsg(err instanceof Error ? err.message : 'Ошибка');
    }
  };

  if (!data) return <LoadingScreen />;

  return (
    <Container className="py-4">
      <PageHeader
        title="Мои отклики"
        subtitle="Отслеживайте статус заявок и переходите к обучению"
        actions={
          <>
            <Link to="/student/profile" className="btn btn-outline-modern btn-sm">
              <i className="bi bi-person-lines-fill me-1" />
              Профиль и резюме
            </Link>
            {data.isVerified && (
              <Link to="/student/job-market" className="btn btn-gradient-accent btn-sm">
                <i className="bi bi-briefcase me-1" />
                Рынок вакансий
              </Link>
            )}
          </>
        }
      />

      {msg && (
        <Alert variant="success" className="alert-modern mb-4" dismissible onClose={() => setMsg('')}>
          {msg}
        </Alert>
      )}

      {data.hasActiveUniversityProgram && (
        <Alert variant="info" className="alert-modern mb-4">
          Вы обучаетесь на одной программе ВУЗа. Другую программу можно выбрать после завершения и
          верификации текущей.
        </Alert>
      )}

      {data.isVerified && (
        <Alert variant="secondary" className="alert-modern mb-4">
          Стажировки: {data.acceptedCompanyInternships} из {data.maxCompanyInternships} принято.
          {data.canTakeMoreInternships
            ? ' Откликаться можно на любые вакансии; компания примет не больше лимита.'
            : ' Лимит стажировок исчерпан — новые отклики возможны, но компания не сможет принять ещё одну.'}
        </Alert>
      )}

      <div className="table-card mb-4">
        <table className="table mb-0">
          <thead>
            <tr>
              <th>Позиция</th>
              <th>Статус</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {data.applications.length === 0 ? (
              <tr>
                <td colSpan={3} className="text-center text-muted py-4">
                  У вас пока нет откликов
                </td>
              </tr>
            ) : (
              data.applications.map((app) => (
                <tr key={app.id}>
                  <td className="fw-semibold">{app.internship.title}</td>
                  <td>
                    <Badge bg="primary" className="badge-modern opacity-90">
                      {app.status}
                    </Badge>
                  </td>
                  <td>
                    <div className="action-bar">
                      {(app.status === 'ACCEPTED' || app.status === 'APPROVED') &&
                        app.internship.companyJob && (
                          <Link
                            to={`/student/chat/${app.internship.id}`}
                            className="btn btn-sm btn-primary"
                          >
                            Чат
                          </Link>
                        )}
                      {!app.internship.companyJob &&
                        (app.status === 'APPROVED' ||
                          app.status === 'COMPLETED' ||
                          app.status === 'VERIFIED') && (
                          <Link
                            to={`/student/learning/${app.id}`}
                            className="btn btn-sm btn-success"
                          >
                            {app.status === 'COMPLETED' || app.status === 'VERIFIED'
                              ? 'Обучение / сертификат'
                              : 'Обучение'}
                          </Link>
                        )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data.companyJobs.length > 0 && (
        <>
          <div className="section-title accent mb-3">
            <span className="icon-wrap">
              <i className="bi bi-lightning-fill" />
            </span>
            Доступные вакансии
          </div>
          <Row className="g-3">
            {data.companyJobs.map((job) => {
              const alreadyApplied = data.appliedInternshipIds.includes(job.id);
              return (
                <Col md={4} key={job.id}>
                  <div className="card card-job accent h-100">
                    <div className="card-body">
                      <h6 className="card-title fw-bold">{job.title}</h6>
                      <p className="small text-muted mb-3">{job.companyName}</p>
                      {alreadyApplied ? (
                        <span className="badge bg-secondary">Отклик отправлен</span>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-gradient-accent btn-sm"
                          onClick={() => apply(job.id)}
                        >
                          Откликнуться
                        </button>
                      )}
                    </div>
                  </div>
                </Col>
              );
            })}
          </Row>
        </>
      )}
    </Container>
  );
}

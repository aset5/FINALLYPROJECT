import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Button, Col, Container, Form, Row } from 'react-bootstrap';
import EditInternshipModal, { type InternshipEditFields } from '../../components/EditInternshipModal';
import StudentProfileModal from '../../components/StudentProfileModal';
import LoadingScreen from '../../components/LoadingScreen';
import PageHeader from '../../components/PageHeader';
import { api } from '../../api/client';
import type { Application, Company, Internship } from '../../types';
import { internshipStatusLabel, internshipStatusVariant } from '../../utils/internshipStatus';

interface DashboardData {
  company: Company | null;
  internships: Internship[];
  candidates: Application[];
}

export default function CompanyDashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [newJob, setNewJob] = useState({ title: '', city: '', description: '' });
  const [resumeStudentId, setResumeStudentId] = useState<number | null>(null);
  const [editingJob, setEditingJob] = useState<Internship | null>(null);

  const load = () => api.get<DashboardData>('/api/company/dashboard').then(setData);

  useEffect(() => {
    load();
  }, []);

  const addJob = async (e: FormEvent) => {
    e.preventDefault();
    await api.post('/api/company/internships', newJob);
    setNewJob({ title: '', city: '', description: '' });
    load();
  };

  const saveJob = async (id: number, fields: InternshipEditFields) => {
    await api.put(`/api/company/internships/${id}`, {
      title: fields.title,
      city: fields.city,
      description: fields.description,
    });
    load();
  };

  if (!data) return <LoadingScreen />;

  if (!data.company?.id) {
    return (
      <Container className="py-5">
        <div className="empty-state card-modern mx-auto" style={{ maxWidth: 480 }}>
          <i className="bi bi-building" />
          <h5>Сначала настройте профиль компании</h5>
          <p className="small">Без профиля нельзя публиковать вакансии</p>
          <Link to="/company/profile" className="btn btn-gradient mt-2">
            Настроить профиль
          </Link>
        </div>
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <PageHeader
        title={data.company.name || 'Компания'}
        subtitle="Панель управления вакансиями и кандидатами"
        actions={
          <Link to="/company/profile" className="btn btn-outline-modern btn-sm">
            <i className="bi bi-gear me-1" />
            Профиль
          </Link>
        }
      />

      <Row className="g-4 mb-4">
        <Col md={5}>
          <div className="card-modern p-4 h-100">
            <h5 className="fw-bold mb-3">
              <i className="bi bi-plus-circle text-primary me-2" />
              Новая вакансия
            </h5>
            <Form onSubmit={addJob}>
              <Form.Control
                className="mb-2"
                placeholder="Название позиции"
                value={newJob.title}
                onChange={(e) => setNewJob({ ...newJob, title: e.target.value })}
                required
              />
              <Form.Control
                className="mb-2"
                placeholder="Город"
                value={newJob.city}
                onChange={(e) => setNewJob({ ...newJob, city: e.target.value })}
              />
              <Form.Control
                as="textarea"
                rows={3}
                className="mb-3"
                placeholder="Описание и требования"
                value={newJob.description}
                onChange={(e) => setNewJob({ ...newJob, description: e.target.value })}
              />
              <button type="submit" className="btn btn-gradient w-100">
                Опубликовать
              </button>
            </Form>
          </div>
        </Col>
        <Col md={7}>
          <div className="card-modern p-4 h-100">
            <h5 className="fw-bold mb-3">
              <i className="bi bi-briefcase text-primary me-2" />
              Мои вакансии
            </h5>
            {data.internships.length === 0 ? (
              <p className="text-muted small mb-0">Вакансий пока нет</p>
            ) : (
              data.internships.map((job) => (
                <div
                  key={job.id}
                  className="d-flex justify-content-between align-items-center py-3 border-bottom"
                >
                  <div>
                    <strong>{job.title}</strong>
                    <Badge bg={internshipStatusVariant(job.status)} className="ms-2 badge-modern">
                      {internshipStatusLabel(job.status)}
                    </Badge>
                    {job.city && <span className="text-muted small ms-2">{job.city}</span>}
                  </div>
                  <div className="d-flex gap-1 flex-wrap justify-content-end">
                    <Button size="sm" variant="outline-primary" onClick={() => setEditingJob(job)}>
                      Изменить
                    </Button>
                    {job.status === 'APPROVED' && (
                      <Button
                        size="sm"
                        variant="outline-warning"
                        onClick={() => api.post(`/api/company/internships/${job.id}/close`).then(load)}
                      >
                        Закрыть
                      </Button>
                    )}
                    {job.status === 'CLOSED' && (
                      <Button
                        size="sm"
                        variant="outline-success"
                        onClick={() => api.post(`/api/company/internships/${job.id}/reopen`).then(load)}
                      >
                        Открыть
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="outline-danger"
                      onClick={() => api.delete(`/api/company/internships/${job.id}`).then(load)}
                    >
                      Удалить
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </Col>
      </Row>

      <div className="section-title primary mb-3">
        <span className="icon-wrap">
          <i className="bi bi-people-fill" />
        </span>
        Кандидаты
      </div>
      <div className="table-card">
        <table className="table mb-0">
          <thead>
            <tr>
              <th>Студент</th>
              <th>Вакансия</th>
              <th>Статус</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {data.candidates.length === 0 ? (
              <tr>
                <td colSpan={4} className="text-center text-muted py-4">
                  Откликов пока нет
                </td>
              </tr>
            ) : (
              data.candidates.map((c) => (
                <tr key={c.id}>
                  <td className="fw-semibold">{c.student?.fullName || c.student?.username}</td>
                  <td>{c.internship.title}</td>
                  <td>
                    <Badge
                      bg={
                        c.status === 'ACCEPTED'
                          ? 'success'
                          : c.status === 'PENDING'
                            ? 'warning'
                            : 'secondary'
                      }
                      className="badge-modern"
                    >
                      {c.status}
                    </Badge>
                  </td>
                  <td>
                    <div className="action-bar">
                      {c.status === 'PENDING' && (
                        <>
                          <Button
                            size="sm"
                            variant="success"
                            onClick={() => api.post(`/api/company/applications/${c.id}/accept`).then(load)}
                          >
                            Принять
                          </Button>
                          <Button
                            size="sm"
                            variant="outline-danger"
                            onClick={() => api.post(`/api/company/applications/${c.id}/reject`).then(load)}
                          >
                            Отклонить
                          </Button>
                        </>
                      )}
                      {c.status === 'ACCEPTED' && c.student && (
                        <Link
                          to={`/company/chat/${c.internship.id}/${c.student.id}`}
                          className="btn btn-sm btn-primary"
                        >
                          <i className="bi bi-chat-dots me-1" />
                          Чат
                        </Link>
                      )}
                      {c.student && (
                        <Button
                          size="sm"
                          className="btn-outline-modern"
                          onClick={() => setResumeStudentId(c.student!.id)}
                        >
                          <i className="bi bi-file-person me-1" />
                          Резюме
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <StudentProfileModal
        studentId={resumeStudentId}
        onClose={() => setResumeStudentId(null)}
        apiBase="/api/company/students"
        downloadKind="company"
        showResumeDownload
      />

      <EditInternshipModal
        show={editingJob != null}
        item={editingJob}
        kind="job"
        onClose={() => setEditingJob(null)}
        onSave={saveJob}
      />
    </Container>
  );
}

import { useCallback, useEffect, useState } from 'react';
import { Badge, Button, Container, Form, Table } from 'react-bootstrap';
import { api } from '../../api/client';
import AdminNav from '../../components/admin/AdminNav';
import PageHeader from '../../components/PageHeader';
import type { Application } from '../../types';

function appStatusBadge(status: string) {
  switch (status) {
    case 'PENDING':
      return 'warning';
    case 'APPROVED':
    case 'ACCEPTED':
      return 'primary';
    case 'COMPLETED':
      return 'info';
    case 'VERIFIED':
      return 'success';
    case 'REJECTED':
      return 'danger';
    default:
      return 'secondary';
  }
}

export default function AdminApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (keyword.trim()) params.set('keyword', keyword.trim());
    if (status) params.set('status', status);
    const qs = params.toString();
    return api
      .get<{ applications: Application[] }>(`/api/admin/applications${qs ? `?${qs}` : ''}`)
      .then((d) => setApplications(d.applications))
      .finally(() => setLoading(false));
  }, [keyword, status]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <Container className="py-4">
      <PageHeader title="Заявки" subtitle="Все отклики студентов на программы и вакансии" />
      <AdminNav />

      <div className="card-modern p-4">
        <Form
          className="row g-2 mb-3"
          onSubmit={(e) => {
            e.preventDefault();
            load();
          }}
        >
          <div className="col-md-5">
            <Form.Control
              placeholder="Студент, программа, ВУЗ..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <div className="col-md-4">
            <Form.Select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">Все статусы</option>
              <option value="PENDING">PENDING</option>
              <option value="APPROVED">APPROVED</option>
              <option value="ACCEPTED">ACCEPTED</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="VERIFIED">VERIFIED</option>
              <option value="REJECTED">REJECTED</option>
            </Form.Select>
          </div>
          <div className="col-md-3">
            <Button type="submit" className="btn-gradient w-100" disabled={loading}>
              Найти
            </Button>
          </div>
        </Form>

        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary" />
          </div>
        ) : applications.length === 0 ? (
          <p className="text-muted mb-0">Заявки не найдены.</p>
        ) : (
          <Table responsive className="mb-0 align-middle">
            <thead>
              <tr>
                <th>ID</th>
                <th>Студент</th>
                <th>Программа / вакансия</th>
                <th>Тип</th>
                <th>Статус</th>
                <th>Оценка</th>
                <th>Дата</th>
              </tr>
            </thead>
            <tbody>
              {applications.map((a) => (
                <tr key={a.id}>
                  <td>{a.id}</td>
                  <td>
                    <div className="fw-semibold small">
                      {a.student?.fullName || a.student?.username}
                    </div>
                    <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                      {a.student?.username}
                    </div>
                  </td>
                  <td>{a.internship.title}</td>
                  <td>{a.internship.companyJob ? 'Компания' : 'ВУЗ'}</td>
                  <td>
                    <Badge bg={appStatusBadge(a.status)}>{a.status}</Badge>
                  </td>
                  <td className="small">
                    {a.finalGradePercent != null ? (
                      <>
                        {a.finalGradePercent}%
                        {a.gradeLetter ? ` · ${a.gradeLetter}` : ''}
                      </>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className="small text-muted">
                    {a.appliedAt
                      ? new Date(a.appliedAt).toLocaleDateString('ru-RU')
                      : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>
    </Container>
  );
}

import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Button, Container, Form, Table } from 'react-bootstrap';
import { api } from '../../api/client';
import AdminNav from '../../components/admin/AdminNav';
import InternshipDetailModal from '../../components/admin/InternshipDetailModal';
import PageHeader from '../../components/PageHeader';
import type { Internship, User } from '../../types';

interface AdminStats {
  totalUsers: number;
  students: number;
  companies: number;
  universityAdmins: number;
  pendingAccountApprovals: number;
  pendingInternships: number;
  approvedInternships: number;
  totalApplications: number;
  completedPrograms: number;
  verifiedPrograms: number;
}

interface DashboardData {
  stats: AdminStats;
  pendingAccountApprovals: User[];
  pendingInternships: Internship[];
  internships: Internship[];
}

function statusBadge(status: string) {
  switch (status) {
    case 'PENDING':
      return 'warning';
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
      return 'danger';
    case 'CLOSED':
      return 'secondary';
    default:
      return 'secondary';
  }
}

export default function AdminDashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('');
  const [type, setType] = useState('all');
  const [detail, setDetail] = useState<Internship | null>(null);

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (keyword.trim()) params.set('keyword', keyword.trim());
    if (status) params.set('status', status);
    if (type && type !== 'all') params.set('type', type);
    const qs = params.toString();
    return api.get<DashboardData>(`/api/admin/dashboard${qs ? `?${qs}` : ''}`).then(setData);
  }, [keyword, status, type]);

  useEffect(() => {
    load();
  }, [load]);

  const approveInternship = async (id: number) => {
    await api.post(`/api/admin/internships/${id}/approve`);
    load();
  };

  const approveUser = async (id: number) => {
    await api.post(`/api/admin/users/${id}/approve`);
    load();
  };

  const reject = async (id: number) => {
    await api.post(`/api/admin/internships/${id}/reject`);
    load();
  };

  const remove = async (id: number) => {
    if (!window.confirm('Удалить стажировку/программу безвозвратно?')) return;
    await api.delete(`/api/admin/internships/${id}`);
    load();
  };

  if (!data) {
    return (
      <Container className="py-5 text-center">
        <div className="spinner-border text-primary" />
      </Container>
    );
  }

  const { stats } = data;

  return (
    <Container className="py-4">
      <PageHeader title="Админ-панель" subtitle="Модерация и обзор платформы INTERN.PRO" />
      <AdminNav />

      <div className="row g-3 mb-4">
        <div className="col-6 col-md-4 col-lg">
          <div className="stat-card">
            <span className="stat-label">Пользователи</span>
            <span className="stat-value">{stats.totalUsers}</span>
          </div>
        </div>
        <div className="col-6 col-md-4 col-lg">
          <div className="stat-card stat-card-accent">
            <span className="stat-label">Аккаунты на проверке</span>
            <span className="stat-value">{stats.pendingAccountApprovals}</span>
          </div>
        </div>
        <div className="col-6 col-md-4 col-lg">
          <div className="stat-card">
            <span className="stat-label">Вакансии / программы</span>
            <span className="stat-value">{stats.pendingInternships}</span>
          </div>
        </div>
        <div className="col-6 col-md-4 col-lg">
          <div className="stat-card">
            <span className="stat-label">Одобрено</span>
            <span className="stat-value">{stats.approvedInternships}</span>
          </div>
        </div>
        <div className="col-6 col-md-4 col-lg">
          <div className="stat-card">
            <span className="stat-label">Заявки</span>
            <span className="stat-value">{stats.totalApplications}</span>
          </div>
        </div>
        <div className="col-6 col-md-4 col-lg">
          <div className="stat-card stat-card-success">
            <span className="stat-label">Сертификаты</span>
            <span className="stat-value">{stats.completedPrograms + stats.verifiedPrograms}</span>
          </div>
        </div>
      </div>

      <p className="text-muted small mb-4">
        Студентов: {stats.students} · Компаний: {stats.companies} · Админов ВУЗа:{' '}
        {stats.universityAdmins}
      </p>

      <div className="card-modern p-4 mb-4">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h5 className="fw-bold mb-0">
            <i className="bi bi-person-check text-warning me-2" />
            Новые аккаунты (компании и ВУЗы)
          </h5>
          <Link to="/admin/users?pending=1" className="btn btn-sm btn-outline-modern">
            Все пользователи
          </Link>
        </div>
        {data.pendingAccountApprovals.length === 0 ? (
          <p className="text-muted mb-0 small">Нет заявок на регистрацию.</p>
        ) : (
          <Table responsive className="mb-0 align-middle">
            <thead>
              <tr>
                <th>Логин</th>
                <th>ФИО / Email</th>
                <th>Роль</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {data.pendingAccountApprovals.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td className="small">
                    <div>{u.fullName || '—'}</div>
                    <div className="text-muted">{u.email || '—'}</div>
                    {u.universityName && <div className="text-muted">{u.universityName}</div>}
                  </td>
                  <td>
                    <Badge bg={u.role === 'COMPANY' ? 'info' : 'warning'} text="dark">
                      {u.role}
                    </Badge>
                  </td>
                  <td className="text-nowrap">
                    <Button size="sm" variant="success" onClick={() => approveUser(u.id)}>
                      Активировать
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>

      <div className="card-modern p-4 mb-4">
        <h5 className="fw-bold mb-3">
          <i className="bi bi-hourglass-split text-warning me-2" />
          Вакансии и программы на модерации
        </h5>
        {data.pendingInternships.length === 0 ? (
          <p className="text-muted mb-0 small">Нет позиций, ожидающих одобрения.</p>
        ) : (
          <Table responsive className="mb-0 align-middle">
            <thead>
              <tr>
                <th>Название</th>
                <th>Тип</th>
                <th>Организация</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {data.pendingInternships.map((i) => (
                <tr key={i.id}>
                  <td>{i.title}</td>
                  <td>{i.companyJob ? 'Компания' : 'ВУЗ'}</td>
                  <td className="small text-muted">
                    {i.companyJob ? i.companyName : i.universityName}
                  </td>
                  <td className="text-nowrap">
                    <Button size="sm" variant="outline-primary" className="me-1" onClick={() => setDetail(i)}>
                      Подробнее
                    </Button>
                    <Button size="sm" variant="success" className="me-1" onClick={() => approveInternship(i.id)}>
                      Одобрить
                    </Button>
                    <Button size="sm" variant="outline-danger" className="me-1" onClick={() => reject(i.id)}>
                      Отклонить
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>

      <div className="card-modern p-4">
        <h5 className="fw-bold mb-3">
          <i className="bi bi-briefcase text-primary me-2" />
          Все программы и вакансии
        </h5>
        <Form
          className="row g-2 mb-3"
          onSubmit={(e) => {
            e.preventDefault();
            load();
          }}
        >
          <div className="col-md-4">
            <Form.Control
              placeholder="Поиск..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <div className="col-md-3">
            <Form.Select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">Все статусы</option>
              <option value="PENDING">На модерации</option>
              <option value="APPROVED">Одобрено</option>
              <option value="REJECTED">Отклонено</option>
              <option value="CLOSED">Закрыто</option>
            </Form.Select>
          </div>
          <div className="col-md-3">
            <Form.Select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="all">Все типы</option>
              <option value="university">Программы ВУЗа</option>
              <option value="company">Вакансии компаний</option>
            </Form.Select>
          </div>
          <div className="col-md-2">
            <Button type="submit" className="btn-gradient w-100">
              Найти
            </Button>
          </div>
        </Form>

        <Table responsive className="mb-0 align-middle">
          <thead>
            <tr>
              <th>Название</th>
              <th>Тип</th>
              <th>Организация</th>
              <th>Город</th>
              <th>Статус</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {data.internships.map((i) => (
              <tr key={i.id}>
                <td>{i.title}</td>
                <td>{i.companyJob ? 'Компания' : 'ВУЗ'}</td>
                <td className="small">{i.companyJob ? i.companyName : i.universityName}</td>
                <td className="small text-muted">{i.city || '—'}</td>
                <td>
                  <Badge bg={statusBadge(i.status)}>{i.status}</Badge>
                </td>
                <td className="text-nowrap">
                  <Button size="sm" variant="outline-primary" className="me-1" onClick={() => setDetail(i)}>
                    Подробнее
                  </Button>
                  {i.status === 'PENDING' && (
                    <>
                      <Button size="sm" variant="success" className="me-1" onClick={() => approveInternship(i.id)}>
                        Одобрить
                      </Button>
                      <Button size="sm" variant="outline-danger" className="me-1" onClick={() => reject(i.id)}>
                        Отклонить
                      </Button>
                    </>
                  )}
                  <Button size="sm" variant="danger" onClick={() => remove(i.id)}>
                    Удалить
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>

      <InternshipDetailModal
        internship={detail}
        onClose={() => setDetail(null)}
        onApprove={approveInternship}
        onReject={reject}
        showModerationActions
      />
    </Container>
  );
}

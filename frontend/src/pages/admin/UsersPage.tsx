import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Alert, Badge, Button, Container, Form, Table } from 'react-bootstrap';
import { api } from '../../api/client';
import AdminNav from '../../components/admin/AdminNav';
import PageHeader from '../../components/PageHeader';
import type { Role, User } from '../../types';

interface UserEdit {
  username: string;
  password: string;
  enabled: boolean;
}

export default function AdminUsersPage() {
  const [searchParams] = useSearchParams();
  const [users, setUsers] = useState<User[]>([]);
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState('');
  const [pendingOnly, setPendingOnly] = useState(searchParams.get('pending') === '1');
  const [edits, setEdits] = useState<Record<number, UserEdit>>({});
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (keyword.trim()) params.set('keyword', keyword.trim());
    if (role) params.set('role', role);
    if (pendingOnly) params.set('pendingOnly', 'true');
    const qs = params.toString();
    return api
      .get<{ users: User[] }>(`/api/admin/users${qs ? `?${qs}` : ''}`)
      .then((d) => {
        setUsers(d.users);
        const initial: Record<number, UserEdit> = {};
        d.users.forEach((u) => {
          initial[u.id] = {
            username: u.username,
            password: '',
            enabled: u.enabled === true,
          };
        });
        setEdits(initial);
      })
      .finally(() => setLoading(false));
  }, [keyword, role, pendingOnly]);

  useEffect(() => {
    load();
  }, [load]);

  const approve = async (id: number) => {
    await api.post(`/api/admin/users/${id}/approve`);
    load();
  };

  const save = async (id: number) => {
    const edit = edits[id];
    await api.put(`/api/admin/users/${id}`, {
      username: edit.username,
      password: edit.password || undefined,
      enabled: edit.enabled,
    });
    if (!edit.enabled) {
      alert('Пользователь заблокирован. Текущая сессия будет сброшена при следующем запросе.');
    }
    load();
  };

  const remove = async (id: number, username: string) => {
    if (!window.confirm(`Удалить пользователя «${username}»? Это действие необратимо.`)) return;
    await api.delete(`/api/admin/users/${id}`);
    load();
  };

  const roleBadge = (r: Role) => {
    switch (r) {
      case 'ADMIN':
        return 'danger';
      case 'UNIVERSITY_ADMIN':
        return 'warning';
      case 'COMPANY':
        return 'info';
      default:
        return 'secondary';
    }
  };

  return (
    <Container className="py-4">
      <PageHeader title="Пользователи" subtitle="Управление аккаунтами платформы" />
      <AdminNav />

      {pendingOnly && (
        <Alert variant="warning" className="mb-3">
          Показаны компании и ВУЗы, ожидающие активации после регистрации.
        </Alert>
      )}

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
              placeholder="Логин, email, ФИО..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <div className="col-md-4">
            <Form.Select value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="">Все роли</option>
              <option value="STUDENT">STUDENT</option>
              <option value="COMPANY">COMPANY</option>
              <option value="UNIVERSITY_ADMIN">UNIVERSITY_ADMIN</option>
              <option value="ADMIN">ADMIN</option>
            </Form.Select>
          </div>
          <div className="col-md-3 d-flex gap-2">
            <Form.Check
              type="checkbox"
              id="pendingOnly"
              className="align-self-center"
              checked={pendingOnly}
              onChange={(e) => setPendingOnly(e.target.checked)}
              label="Ожидают активации"
            />
            <Button type="submit" className="btn-gradient flex-grow-1" disabled={loading}>
              Найти
            </Button>
          </div>
        </Form>

        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary" />
          </div>
        ) : (
          <Table responsive className="mb-0 align-middle">
            <thead>
              <tr>
                <th>ID</th>
                <th>Логин</th>
                <th>ФИО / Email</th>
                <th>Роль</th>
                <th>Активен</th>
                <th>Пароль</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr
                  key={u.id}
                  className={
                    !u.enabled && (u.role === 'COMPANY' || u.role === 'UNIVERSITY_ADMIN')
                      ? 'table-warning'
                      : undefined
                  }
                >
                  <td>{u.id}</td>
                  <td>
                    <Form.Control
                      size="sm"
                      value={edits[u.id]?.username || ''}
                      onChange={(e) =>
                        setEdits({
                          ...edits,
                          [u.id]: { ...edits[u.id], username: e.target.value },
                        })
                      }
                    />
                  </td>
                  <td className="small">
                    <div>{u.fullName || '—'}</div>
                    <div className="text-muted">{u.email || '—'}</div>
                    {u.universityName && (
                      <div className="text-muted">{u.universityName}</div>
                    )}
                  </td>
                  <td>
                    <Badge bg={roleBadge(u.role)}>{u.role}</Badge>
                  </td>
                  <td>
                    {u.role === 'ADMIN' ? (
                      <Badge bg="success">Всегда активен</Badge>
                    ) : !u.enabled && (u.role === 'COMPANY' || u.role === 'UNIVERSITY_ADMIN') ? (
                      <Badge bg="warning" text="dark">
                        Ожидает активации
                      </Badge>
                    ) : (
                      <Form.Check
                        type="switch"
                        checked={edits[u.id]?.enabled ?? false}
                        onChange={(e) =>
                          setEdits({
                            ...edits,
                            [u.id]: { ...edits[u.id], enabled: e.target.checked },
                          })
                        }
                        label={edits[u.id]?.enabled ? 'Да' : 'Нет'}
                      />
                    )}
                  </td>
                  <td>
                    <Form.Control
                      size="sm"
                      type="password"
                      placeholder="новый"
                      value={edits[u.id]?.password || ''}
                      onChange={(e) =>
                        setEdits({
                          ...edits,
                          [u.id]: { ...edits[u.id], password: e.target.value },
                        })
                      }
                    />
                  </td>
                  <td className="text-nowrap">
                    {!u.enabled && (u.role === 'COMPANY' || u.role === 'UNIVERSITY_ADMIN') && (
                      <Button size="sm" variant="success" className="me-1" onClick={() => approve(u.id)}>
                        Активировать
                      </Button>
                    )}
                    <Button size="sm" className="me-1 btn-gradient" onClick={() => save(u.id)}>
                      Сохранить
                    </Button>
                    <Button
                      size="sm"
                      variant="outline-danger"
                      onClick={() => remove(u.id, u.username)}
                      disabled={u.role === 'ADMIN'}
                    >
                      Удалить
                    </Button>
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

import { useCallback, useEffect, useState } from 'react';
import { Badge, Button, Container, Form, Table } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import AdminNav from '../../components/admin/AdminNav';
import PageHeader from '../../components/PageHeader';

interface AdminCertificate {
  applicationId: number;
  certificateNumber: string;
  studentName: string;
  studentUsername: string;
  programTitle: string;
  universityName?: string;
  finalGradePercent?: number;
  gradeLetter?: string;
  completedAt?: string;
  status: string;
}

export default function AdminCertificatesPage() {
  const [certificates, setCertificates] = useState<AdminCertificate[]>([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const qs = keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : '';
    return api
      .get<{ certificates: AdminCertificate[] }>(`/api/admin/certificates${qs}`)
      .then((d) => setCertificates(d.certificates))
      .finally(() => setLoading(false));
  }, [keyword]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <Container className="py-4">
      <PageHeader title="Реестр сертификатов" subtitle="Завершённые программы ВУЗа с оценками" />
      <AdminNav />

      <div className="card-modern p-4">
        <Form
          className="d-flex gap-2 mb-3"
          onSubmit={(e) => {
            e.preventDefault();
            load();
          }}
        >
          <Form.Control
            placeholder="Номер, студент, программа..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <Button type="submit" className="btn-gradient" disabled={loading}>
            Найти
          </Button>
        </Form>

        {loading ? (
          <div className="text-center py-4">
            <div className="spinner-border text-primary" />
          </div>
        ) : certificates.length === 0 ? (
          <p className="text-muted mb-0">Сертификаты не найдены.</p>
        ) : (
          <Table responsive className="mb-0 align-middle">
            <thead>
              <tr>
                <th>Номер</th>
                <th>Студент</th>
                <th>Программа</th>
                <th>ВУЗ</th>
                <th>Оценка</th>
                <th>Статус</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {certificates.map((c) => (
                <tr key={c.applicationId}>
                  <td>
                    <code className="small">{c.certificateNumber}</code>
                  </td>
                  <td>
                    <div className="fw-semibold small">{c.studentName}</div>
                    <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                      {c.studentUsername}
                    </div>
                  </td>
                  <td>{c.programTitle}</td>
                  <td className="small">{c.universityName || '—'}</td>
                  <td className="small">
                    {c.finalGradePercent != null ? (
                      <>
                        {c.finalGradePercent}%
                        {c.gradeLetter ? ` · ${c.gradeLetter}` : ''}
                      </>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>
                    <Badge bg={c.status === 'VERIFIED' ? 'success' : 'info'}>{c.status}</Badge>
                  </td>
                  <td className="text-nowrap">
                    <Link
                      to={`/verify/${encodeURIComponent(c.certificateNumber)}`}
                      className="btn btn-outline-modern btn-sm"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Проверить
                    </Link>
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

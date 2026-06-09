import { useEffect, useState, type FormEvent } from 'react';
import { Badge, Button, Card, Col, Container, Form, Row, Table } from 'react-bootstrap';
import { api, downloadFile } from '../../api/client';
import EditInternshipModal, { type InternshipEditFields } from '../../components/EditInternshipModal';
import StudentProfileModal from '../../components/StudentProfileModal';
import ProgramContentPanel from '../../components/university/ProgramContentPanel';
import type { Application, Internship } from '../../types';
import { internshipStatusLabel, internshipStatusVariant } from '../../utils/internshipStatus';

interface DashboardData {
  university: { id: number; name: string };
  internships: Internship[];
  applications: Application[];
}

export default function UniversityDashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [form, setForm] = useState({ title: '', description: '', maxPlaces: 10, studyMaterials: '' });
  const [contentProgram, setContentProgram] = useState<Internship | null>(null);
  const [editingProgram, setEditingProgram] = useState<Internship | null>(null);
  const [profileStudentId, setProfileStudentId] = useState<number | null>(null);

  const load = () => api.get<DashboardData>('/api/university-admin/dashboard').then(setData);

  useEffect(() => {
    load();
  }, []);

  const addProgram = async (e: FormEvent) => {
    e.preventDefault();
    await api.post('/api/university-admin/internships', form);
    setForm({ title: '', description: '', maxPlaces: 10, studyMaterials: '' });
    load();
  };

  const verify = async (id: number) => {
    await api.post(`/api/university-admin/applications/${id}/verify`);
    load();
  };

  const saveProgram = async (id: number, fields: InternshipEditFields) => {
    await api.put(`/api/university-admin/internships/${id}`, {
      title: fields.title,
      description: fields.description,
      studyMaterials: fields.studyMaterials,
      maxPlaces: fields.maxPlaces,
    });
    load();
    if (contentProgram?.id === id) {
      setContentProgram((prev) =>
        prev
          ? {
              ...prev,
              title: fields.title,
              description: fields.description,
              studyMaterials: fields.studyMaterials,
              maxPlaces: fields.maxPlaces ?? prev.maxPlaces,
              status: 'PENDING',
            }
          : prev,
      );
    }
  };

  if (!data) return <div className="text-center py-5"><div className="spinner-border" /></div>;

  const programFinished = (status: string) => status === 'COMPLETED' || status === 'VERIFIED';

  return (
    <Container className="py-4">
      <h2 className="fw-bold mb-4">{data.university.name} — панель администратора</h2>

      <Row className="g-4 mb-4">
        <Col md={5}>
          <Card className="border-0 shadow-sm">
            <Card.Body>
              <h5>Новая программа</h5>
              <Form onSubmit={addProgram}>
                <Form.Control
                  className="mb-2"
                  placeholder="Название"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  required
                />
                <Form.Control
                  as="textarea"
                  className="mb-2"
                  placeholder="Описание"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                />
                <Form.Control
                  as="textarea"
                  className="mb-2"
                  placeholder="Учебные материалы"
                  value={form.studyMaterials}
                  onChange={(e) => setForm({ ...form, studyMaterials: e.target.value })}
                />
                <Form.Control
                  type="number"
                  className="mb-2"
                  value={form.maxPlaces}
                  onChange={(e) => setForm({ ...form, maxPlaces: Number(e.target.value) })}
                />
                <Button type="submit" className="w-100">Добавить (на модерацию)</Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>
        <Col md={7}>
          <Card className="border-0 shadow-sm">
            <Card.Body>
              <h5>Программы</h5>
              {data.internships.map((i) => (
                <div key={i.id} className="d-flex justify-content-between align-items-center py-2 border-bottom gap-2">
                  <span>
                    {i.title}{' '}
                    <Badge bg={internshipStatusVariant(i.status)}>{internshipStatusLabel(i.status)}</Badge>
                  </span>
                  <div className="d-flex gap-1">
                    <Button size="sm" variant="outline-secondary" onClick={() => setEditingProgram(i)}>
                      Изменить
                    </Button>
                    <Button size="sm" variant="outline-primary" onClick={() => setContentProgram(i)}>
                      Контент
                    </Button>
                    <Button size="sm" variant="outline-danger" onClick={() => api.delete(`/api/university-admin/internships/${i.id}`).then(load)}>
                      Удалить
                    </Button>
                  </div>
                </div>
              ))}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Card className="border-0 shadow-sm">
        <Card.Body>
          <h5>Заявки студентов</h5>
          <Table responsive>
            <thead>
              <tr>
                <th>Студент</th>
                <th>Программа</th>
                <th>Статус</th>
                <th>Оценка</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {data.applications.map((a) => (
                <tr key={a.id}>
                  <td>
                    <button
                      type="button"
                      className="btn btn-link btn-sm p-0 text-start text-decoration-none"
                      onClick={() => a.student?.id && setProfileStudentId(a.student.id)}
                    >
                      {a.student?.fullName || a.student?.username}
                    </button>
                  </td>
                  <td>{a.internship.title}</td>
                  <td><Badge bg="secondary">{a.status}</Badge></td>
                  <td>
                    {programFinished(a.status) && a.finalGradePercent != null ? (
                      <span className="small fw-semibold">
                        {a.finalGradePercent}%
                        {a.gradeLetter ? ` · ${a.gradeLetter}` : ''}
                      </span>
                    ) : (
                      <span className="text-muted small">—</span>
                    )}
                  </td>
                  <td className="text-nowrap">
                    {a.student?.id && (
                      <Button
                        size="sm"
                        variant="outline-primary"
                        className="me-1"
                        onClick={() => setProfileStudentId(a.student!.id!)}
                      >
                        Профиль
                      </Button>
                    )}
                    {programFinished(a.status) && (
                      <Button
                        size="sm"
                        variant="outline-secondary"
                        className="me-1"
                        onClick={() =>
                          downloadFile(
                            `/api/university-admin/applications/${a.id}/certificate`,
                            `certificate-${a.id}.pdf`,
                          ).catch((err) => alert(err instanceof Error ? err.message : 'Ошибка'))
                        }
                      >
                        PDF
                      </Button>
                    )}
                    {a.status === 'COMPLETED' && (
                      <Button size="sm" onClick={() => verify(a.id)}>
                        Верифицировать
                      </Button>
                    )}
                    {a.status === 'VERIFIED' && (
                      <Badge bg="success" className="ms-1">
                        Верифицирован
                      </Badge>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Card.Body>
      </Card>

      <ProgramContentPanel
        internshipId={contentProgram?.id ?? null}
        internshipTitle={contentProgram?.title}
        onClose={() => setContentProgram(null)}
        onProgramUpdated={load}
      />

      <EditInternshipModal
        show={editingProgram != null}
        item={editingProgram}
        kind="program"
        onClose={() => setEditingProgram(null)}
        onSave={saveProgram}
      />

      <StudentProfileModal
        studentId={profileStudentId}
        onClose={() => setProfileStudentId(null)}
        apiBase="/api/university-admin/students"
        downloadKind="university"
      />
    </Container>
  );
}

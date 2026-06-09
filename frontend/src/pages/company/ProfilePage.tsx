import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Container, Form } from 'react-bootstrap';
import { api } from '../../api/client';
import type { Company } from '../../types';

export default function CompanyProfilePage() {
  const navigate = useNavigate();
  const [company, setCompany] = useState<Company>({ name: '', bin: '' });

  useEffect(() => {
    api.get<Company>('/api/company/profile').then(setCompany);
  }, []);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await api.post('/api/company/profile', company);
    navigate('/company/dashboard');
  };

  return (
    <Container className="py-4" style={{ maxWidth: 500 }}>
      <Card className="border-0 shadow-sm p-3">
        <h3 className="mb-3">Профиль компании</h3>
        <Form onSubmit={onSubmit}>
          <Form.Group className="mb-3">
            <Form.Label>Название</Form.Label>
            <Form.Control
              value={company.name || ''}
              onChange={(e) => setCompany({ ...company, name: e.target.value })}
              required
            />
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>БИН</Form.Label>
            <Form.Control
              value={company.bin || ''}
              onChange={(e) => setCompany({ ...company, bin: e.target.value })}
            />
          </Form.Group>
          <Button type="submit" className="w-100">Сохранить</Button>
        </Form>
      </Card>
    </Container>
  );
}

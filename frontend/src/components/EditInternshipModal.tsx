import { useEffect, useState, type FormEvent } from 'react';
import { Alert, Button, Form, Modal } from 'react-bootstrap';
import type { Internship } from '../types';

export interface InternshipEditFields {
  title: string;
  description: string;
  city?: string;
  studyMaterials?: string;
  maxPlaces?: number;
}

interface Props {
  show: boolean;
  item: Internship | null;
  kind: 'job' | 'program';
  onClose: () => void;
  onSave: (id: number, fields: InternshipEditFields) => Promise<void>;
}

export default function EditInternshipModal({ show, item, kind, onClose, onSave }: Props) {
  const [fields, setFields] = useState<InternshipEditFields>({
    title: '',
    description: '',
    city: '',
    studyMaterials: '',
    maxPlaces: 10,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!item) return;
    setFields({
      title: item.title || '',
      description: item.description || '',
      city: item.city || '',
      studyMaterials: item.studyMaterials || '',
      maxPlaces: item.maxPlaces ?? 10,
    });
    setError('');
  }, [item]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!item) return;
    setSaving(true);
    setError('');
    try {
      await onSave(item.id, fields);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка сохранения');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal show={show} onHide={onClose} centered>
      <Modal.Header closeButton>
        <Modal.Title>{kind === 'job' ? 'Редактировать вакансию' : 'Редактировать программу'}</Modal.Title>
      </Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          <Alert variant="info" className="small py-2">
            После сохранения {kind === 'job' ? 'вакансия' : 'программа'} снова уйдёт на модерацию администратору.
          </Alert>
          {error && <Alert variant="danger">{error}</Alert>}
          <Form.Control
            className="mb-2"
            placeholder="Название"
            value={fields.title}
            onChange={(e) => setFields({ ...fields, title: e.target.value })}
            required
          />
          {kind === 'job' && (
            <Form.Control
              className="mb-2"
              placeholder="Город"
              value={fields.city || ''}
              onChange={(e) => setFields({ ...fields, city: e.target.value })}
            />
          )}
          <Form.Control
            as="textarea"
            rows={3}
            className="mb-2"
            placeholder="Описание"
            value={fields.description}
            onChange={(e) => setFields({ ...fields, description: e.target.value })}
          />
          {kind === 'program' && (
            <>
              <Form.Control
                as="textarea"
                rows={2}
                className="mb-2"
                placeholder="Учебные материалы"
                value={fields.studyMaterials || ''}
                onChange={(e) => setFields({ ...fields, studyMaterials: e.target.value })}
              />
              <Form.Control
                type="number"
                className="mb-2"
                min={1}
                value={fields.maxPlaces ?? 10}
                onChange={(e) => setFields({ ...fields, maxPlaces: Number(e.target.value) })}
              />
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose} disabled={saving}>
            Отмена
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? 'Сохранение…' : 'Сохранить'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

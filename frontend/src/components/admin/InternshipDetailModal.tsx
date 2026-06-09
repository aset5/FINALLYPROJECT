import { Badge, Button, Modal } from 'react-bootstrap';
import type { Internship } from '../../types';

interface Props {
  internship: Internship | null;
  onClose: () => void;
  onApprove?: (id: number) => void;
  onReject?: (id: number) => void;
  showModerationActions?: boolean;
}

export default function InternshipDetailModal({
  internship,
  onClose,
  onApprove,
  onReject,
  showModerationActions,
}: Props) {
  return (
    <Modal show={internship != null} onHide={onClose} centered size="lg" contentClassName="modal-glass">
      <Modal.Header closeButton>
        <Modal.Title>{internship?.title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {internship && (
          <>
            <div className="d-flex flex-wrap gap-2 mb-3">
              <Badge bg="secondary">{internship.status}</Badge>
              <Badge bg={internship.companyJob ? 'info' : 'warning'}>
                {internship.companyJob ? 'Компания' : 'ВУЗ'}
              </Badge>
              {internship.city && <Badge bg="light" text="dark">{internship.city}</Badge>}
            </div>
            <p className="mb-2">
              <strong>Организация:</strong>{' '}
              {internship.companyJob ? internship.companyName : internship.universityName}
            </p>
            {internship.maxPlaces > 0 && (
              <p className="mb-2">
                <strong>Места:</strong> {internship.joinedCount} / {internship.maxPlaces}
              </p>
            )}
            <h6 className="fw-bold mt-3">Описание</h6>
            <p className="text-muted" style={{ whiteSpace: 'pre-wrap' }}>
              {internship.description || '—'}
            </p>
            {!internship.companyJob && internship.studyMaterials && (
              <>
                <h6 className="fw-bold mt-3">Учебные материалы</h6>
                <p className="text-muted small" style={{ whiteSpace: 'pre-wrap' }}>
                  {internship.studyMaterials}
                </p>
              </>
            )}
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        {showModerationActions && internship?.status === 'PENDING' && (
          <>
            <Button
              variant="success"
              onClick={() => {
                onApprove?.(internship.id);
                onClose();
              }}
            >
              Одобрить
            </Button>
            <Button
              variant="outline-danger"
              onClick={() => {
                onReject?.(internship.id);
                onClose();
              }}
            >
              Отклонить
            </Button>
          </>
        )}
        <Button variant="light" onClick={onClose}>
          Закрыть
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

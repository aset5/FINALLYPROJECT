export function internshipStatusLabel(status: string): string {
  switch (status) {
    case 'PENDING':
      return 'На модерации';
    case 'APPROVED':
      return 'Одобрено';
    case 'REJECTED':
      return 'Отклонено';
    case 'CLOSED':
      return 'Закрыто';
    default:
      return status;
  }
}

export function internshipStatusVariant(status: string): string {
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

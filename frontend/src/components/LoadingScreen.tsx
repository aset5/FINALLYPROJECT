export default function LoadingScreen({ label = 'Загрузка...' }: { label?: string }) {
  return (
    <div className="loading-page">
      <div className="spinner-border" role="status" />
      <span>{label}</span>
    </div>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Col, Container, Form, Modal, Row } from 'react-bootstrap';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import LoadingScreen from '../components/LoadingScreen';
import type { Internship } from '../types';

interface HomeData {
  uniPrograms: Internship[];
  companyJobs: Internship[];
  isVerified: boolean;
  hasActiveUniversityProgram?: boolean;
  appliedInternshipIds?: number[];
}

type TypeFilter = 'all' | 'programs' | 'jobs';
type SortOption = 'title-asc' | 'title-desc' | 'city';

function matchesSearch(job: Internship, query: string): boolean {
  const q = query.trim().toLowerCase();
  if (!q) return true;
  const haystack = [
    job.title,
    job.description,
    job.city,
    job.universityName,
    job.companyName,
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
  return haystack.includes(q);
}

function matchesCity(job: Internship, city: string): boolean {
  if (!city) return true;
  return (job.city ?? '').toLowerCase() === city.toLowerCase();
}

function matchesOrg(job: Internship, org: string, kind: 'program' | 'job'): boolean {
  if (!org) return true;
  const name = kind === 'program' ? job.universityName : job.companyName;
  return name === org;
}

function sortJobs(jobs: Internship[], sort: SortOption): Internship[] {
  const list = [...jobs];
  list.sort((a, b) => {
    if (sort === 'city') {
      return (a.city ?? 'яяя').localeCompare(b.city ?? 'яяя', 'ru');
    }
    const cmp = (a.title ?? '').localeCompare(b.title ?? '', 'ru');
    return sort === 'title-desc' ? -cmp : cmp;
  });
  return list;
}

function uniqueSorted(values: (string | undefined)[]): string[] {
  return [...new Set(values.filter((v): v is string => Boolean(v?.trim())))].sort((a, b) =>
    a.localeCompare(b, 'ru'),
  );
}

function JobCard({
  job,
  variant,
  onSelect,
}: {
  job: Internship;
  variant: 'primary' | 'accent';
  onSelect: (job: Internship) => void;
}) {
  const isProgram = variant === 'primary';
  return (
    <Col key={job.id} md={4}>
      <div className={`card card-job${variant === 'accent' ? ' accent' : ''} h-100`}>
        <div className="card-body">
          <span className={`badge-modern ${variant} mb-2`}>{isProgram ? 'Обучение' : 'Работа'}</span>
          <h5 className="card-title">{job.title}</h5>
          <p className="text-muted small mb-3">
            <i className="bi bi-building me-1" />
            {isProgram ? job.universityName : job.companyName}
            {!isProgram && job.city && ` · ${job.city}`}
          </p>
          {job.description && (
            <p className="text-muted small line-clamp-3 mb-3">{job.description}</p>
          )}
          <button
            type="button"
            className={`btn btn-sm ${isProgram ? 'btn-outline-modern' : 'btn-gradient-accent'}`}
            onClick={() => onSelect(job)}
          >
            Подробнее
          </button>
        </div>
      </div>
    </Col>
  );
}

export default function HomePage() {
  const { user } = useAuth();
  const [data, setData] = useState<HomeData | null>(null);
  const [selected, setSelected] = useState<Internship | null>(null);
  const [message, setMessage] = useState('');

  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('all');
  const [cityFilter, setCityFilter] = useState('');
  const [orgFilter, setOrgFilter] = useState('');
  const [sort, setSort] = useState<SortOption>('title-asc');

  const load = () => api.get<HomeData>('/api/home').then(setData);
  useEffect(() => {
    load();
  }, []);

  const cities = useMemo(
    () => (data ? uniqueSorted(data.companyJobs.map((j) => j.city)) : []),
    [data],
  );

  const universities = useMemo(
    () => (data ? uniqueSorted(data.uniPrograms.map((j) => j.universityName)) : []),
    [data],
  );

  const companies = useMemo(
    () => (data ? uniqueSorted(data.companyJobs.map((j) => j.companyName)) : []),
    [data],
  );

  const orgOptions = useMemo(() => {
    if (!data) return [];
    if (typeFilter === 'programs') return universities;
    if (typeFilter === 'jobs') return companies;
    return [...new Set([...universities, ...companies])].sort((a, b) => a.localeCompare(b, 'ru'));
  }, [data, typeFilter, universities, companies]);

  useEffect(() => {
    if (orgFilter && !orgOptions.includes(orgFilter)) {
      setOrgFilter('');
    }
  }, [orgFilter, orgOptions]);

  const filteredPrograms = useMemo(() => {
    if (!data) return [];
    let list = data.uniPrograms.filter(
      (j) => matchesSearch(j, search) && matchesOrg(j, orgFilter, 'program'),
    );
    return sortJobs(list, sort);
  }, [data, search, orgFilter, sort]);

  const filteredJobs = useMemo(() => {
    if (!data) return [];
    let list = data.companyJobs.filter(
      (j) =>
        matchesSearch(j, search) &&
        matchesCity(j, cityFilter) &&
        matchesOrg(j, orgFilter, 'job'),
    );
    return sortJobs(list, sort);
  }, [data, search, cityFilter, orgFilter, sort]);

  const showPrograms = typeFilter === 'all' || typeFilter === 'programs';
  const showJobs = typeFilter === 'all' || typeFilter === 'jobs';
  const canSeeJobs = !user || user.role !== 'STUDENT' || data?.isVerified;

  const hasActiveFilters =
    search.trim() !== '' ||
    typeFilter !== 'all' ||
    cityFilter !== '' ||
    orgFilter !== '' ||
    sort !== 'title-asc';

  const resetFilters = () => {
    setSearch('');
    setTypeFilter('all');
    setCityFilter('');
    setOrgFilter('');
    setSort('title-asc');
  };

  const apply = async (id: number) => {
    if (!user) {
      window.location.href = '/login';
      return;
    }
    try {
      await api.post(`/api/student/apply/${id}`);
      setMessage('Заявка отправлена!');
      setSelected(null);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Ошибка');
    }
  };

  if (!data) return <LoadingScreen />;

  const totalVisible =
    (showPrograms ? filteredPrograms.length : 0) +
    (showJobs && canSeeJobs ? filteredJobs.length : 0);

  return (
    <>
      <section className="hero">
        <Container className="hero-content">
          <h1>Твой путь к карьере</h1>
          <p>Обучайся в ВУЗе — стажируйся и работай в топовых компаниях Казахстана</p>
          <div className="d-flex flex-wrap justify-content-center gap-2">
            {!user && (
              <a href="/register" className="btn btn-gradient btn-lg px-4">
                Начать бесплатно
              </a>
            )}
            <Link to="/verify" className="btn btn-outline-light btn-lg px-4">
              <i className="bi bi-patch-check me-2" />
              Проверить сертификат
            </Link>
          </div>
          <div className="hero-stats">
            <div className="hero-stat">
              <strong>{data.uniPrograms.length}</strong>
              <span>программ</span>
            </div>
            <div className="hero-stat">
              <strong>{data.companyJobs.length}</strong>
              <span>вакансий</span>
            </div>
          </div>
        </Container>
      </section>

      <Container className="py-5">
        {message && (
          <Alert variant="success" className="alert-modern mb-4" dismissible onClose={() => setMessage('')}>
            {message}
          </Alert>
        )}

        <div className="home-filters">
          <Row className="g-3 align-items-end">
            <Col lg={4}>
              <Form.Label>Поиск</Form.Label>
              <div className="input-group">
                <span className="input-group-text bg-white border-end-0">
                  <i className="bi bi-search text-muted" />
                </span>
                <Form.Control
                  type="search"
                  placeholder="Название, город, компания, ВУЗ..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="border-start-0"
                />
              </div>
            </Col>
            <Col sm={6} lg={2}>
              <Form.Label>Город</Form.Label>
              <Form.Select
                value={cityFilter}
                onChange={(e) => setCityFilter(e.target.value)}
                disabled={typeFilter === 'programs' || cities.length === 0}
              >
                <option value="">Все города</option>
                {cities.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </Form.Select>
            </Col>
            <Col sm={6} lg={2}>
              <Form.Label>{typeFilter === 'programs' ? 'ВУЗ' : typeFilter === 'jobs' ? 'Компания' : 'Организация'}</Form.Label>
              <Form.Select value={orgFilter} onChange={(e) => setOrgFilter(e.target.value)}>
                <option value="">Все</option>
                {orgOptions.map((o) => (
                  <option key={o} value={o}>
                    {o}
                  </option>
                ))}
              </Form.Select>
            </Col>
            <Col sm={6} lg={2}>
              <Form.Label>Сортировка</Form.Label>
              <Form.Select value={sort} onChange={(e) => setSort(e.target.value as SortOption)}>
                <option value="title-asc">Название А–Я</option>
                <option value="title-desc">Название Я–А</option>
                <option value="city">По городу</option>
              </Form.Select>
            </Col>
            <Col sm={6} lg={2} className="d-flex flex-column">
              <Form.Label className="d-none d-lg-block">&nbsp;</Form.Label>
              {hasActiveFilters && (
                <button type="button" className="btn btn-outline-modern btn-sm w-100" onClick={resetFilters}>
                  <i className="bi bi-x-circle me-1" />
                  Сбросить
                </button>
              )}
            </Col>
          </Row>

          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mt-3 pt-3 border-top">
            <div className="home-filter-type">
              <span className="small text-muted me-1 align-self-center">Показать:</span>
              {(
                [
                  { id: 'all' as const, label: 'Всё', icon: 'bi-grid' },
                  { id: 'programs' as const, label: 'Обучение', icon: 'bi-mortarboard' },
                  { id: 'jobs' as const, label: 'Вакансии', icon: 'bi-briefcase' },
                ] as const
              ).map(({ id, label, icon }) => (
                <button
                  key={id}
                  type="button"
                  className={`btn btn-sm ${typeFilter === id ? 'btn-gradient' : 'btn-outline-modern'}`}
                  onClick={() => {
                    setTypeFilter(id);
                    if (id === 'programs') setCityFilter('');
                  }}
                >
                  <i className={`bi ${icon} me-1`} />
                  {label}
                </button>
              ))}
            </div>
            <p className="home-filter-meta mb-0">
              Найдено: <strong>{totalVisible}</strong>
              {hasActiveFilters && (
                <span className="ms-1">
                  из {data.uniPrograms.length + (canSeeJobs ? data.companyJobs.length : 0)}
                </span>
              )}
            </p>
          </div>
        </div>

        {showPrograms && (
          <>
            <div className="section-title primary">
              <span className="icon-wrap">
                <i className="bi bi-mortarboard-fill" />
              </span>
              Программы обучения
              {hasActiveFilters && (
                <span className="badge bg-secondary-subtle text-secondary ms-2 fw-semibold">
                  {filteredPrograms.length}
                </span>
              )}
            </div>
            <Row className="g-4 mb-5">
              {filteredPrograms.length === 0 ? (
                <Col xs={12}>
                  <div className="empty-state card-modern p-4">
                    <i className="bi bi-journal-x d-block" />
                    {data.uniPrograms.length === 0
                      ? 'Пока нет доступных программ для вашего университета'
                      : 'Ничего не найдено — измените фильтры или сбросьте поиск'}
                  </div>
                </Col>
              ) : (
                filteredPrograms.map((job) => (
                  <JobCard key={job.id} job={job} variant="primary" onSelect={setSelected} />
                ))
              )}
            </Row>
          </>
        )}

        {showJobs && (
          <>
            <div className="section-title accent">
              <span className="icon-wrap">
                <i className="bi bi-briefcase-fill" />
              </span>
              Вакансии компаний
              {hasActiveFilters && canSeeJobs && (
                <span className="badge bg-secondary-subtle text-secondary ms-2 fw-semibold">
                  {filteredJobs.length}
                </span>
              )}
            </div>
            {!canSeeJobs && user?.role === 'STUDENT' ? (
              <Alert variant="warning" className="alert-modern border-0">
                <i className="bi bi-lock-fill me-2" />
                Вакансии компаний доступны после верификации университетом.
              </Alert>
            ) : (
              <Row className="g-4">
                {filteredJobs.length === 0 ? (
                  <Col xs={12}>
                    <div className="empty-state card-modern p-4">
                      <i className="bi bi-briefcase d-block" />
                      {data.companyJobs.length === 0
                        ? 'Нет открытых вакансий'
                        : 'Ничего не найдено — измените фильтры или сбросьте поиск'}
                    </div>
                  </Col>
                ) : (
                  filteredJobs.map((job) => (
                    <JobCard key={job.id} job={job} variant="accent" onSelect={setSelected} />
                  ))
                )}
              </Row>
            )}
          </>
        )}
      </Container>

      <Modal
        show={!!selected}
        onHide={() => setSelected(null)}
        centered
        contentClassName="modal-glass"
      >
        <Modal.Header closeButton>
          <Modal.Title>{selected?.title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selected && (
            <>
              {(selected.companyName || selected.universityName || selected.city) && (
                <p className="text-muted small mb-2">
                  {selected.universityName && (
                    <>
                      <i className="bi bi-mortarboard me-1" />
                      {selected.universityName}
                    </>
                  )}
                  {selected.companyName && (
                    <>
                      <i className="bi bi-building me-1" />
                      {selected.companyName}
                    </>
                  )}
                  {selected.city && ` · ${selected.city}`}
                </p>
              )}
              <p className="text-muted" style={{ lineHeight: 1.7 }}>
                {selected.description || 'Описание не указано'}
              </p>
              {user?.role === 'STUDENT' && selected && (() => {
                const isProgram = !selected.companyJob;
                const alreadyApplied = data?.appliedInternshipIds?.includes(selected.id) ?? false;
                const canEnrollProgram =
                  isProgram && !alreadyApplied && !data?.hasActiveUniversityProgram;
                const canApplyJob = !isProgram && data?.isVerified && !alreadyApplied;

                if (alreadyApplied) {
                  return (
                    <p className="text-muted small mb-0 mt-2 text-center">
                      Вы уже подали заявку на эту позицию
                    </p>
                  );
                }
                if (isProgram && data?.hasActiveUniversityProgram) {
                  return (
                    <Alert variant="warning" className="small mb-0 mt-2 py-2">
                      Можно обучаться только на одной программе ВУЗа. Завершите текущую и дождитесь
                      верификации.
                    </Alert>
                  );
                }
                if (!isProgram && !data?.isVerified) {
                  return (
                    <Alert variant="warning" className="small mb-0 mt-2 py-2">
                      Отклики на вакансии доступны после верификации программы ВУЗом.
                    </Alert>
                  );
                }
                if (canEnrollProgram || canApplyJob) {
                  return (
                    <button
                      type="button"
                      className="btn btn-gradient w-100 mt-2"
                      onClick={() => apply(selected.id)}
                    >
                      {isProgram ? 'Записаться на программу' : 'Откликнуться'}
                    </button>
                  );
                }
                return null;
              })()}
            </>
          )}
        </Modal.Body>
      </Modal>
    </>
  );
}

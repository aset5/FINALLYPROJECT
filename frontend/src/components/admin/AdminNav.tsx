import { NavLink } from 'react-router-dom';

const links = [
  { to: '/admin/dashboard', label: 'Обзор', icon: 'bi-grid-1x2' },
  { to: '/admin/applications', label: 'Заявки', icon: 'bi-inbox' },
  { to: '/admin/certificates', label: 'Сертификаты', icon: 'bi-award' },
  { to: '/admin/users', label: 'Пользователи', icon: 'bi-people' },
];

export default function AdminNav() {
  return (
    <nav className="admin-nav mb-4">
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          className={({ isActive }) => `admin-nav__link${isActive ? ' admin-nav__link--active' : ''}`}
        >
          <i className={`bi ${link.icon} me-2`} />
          {link.label}
        </NavLink>
      ))}
    </nav>
  );
}

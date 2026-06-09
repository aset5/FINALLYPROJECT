import { Link, useNavigate } from 'react-router-dom';
import { Container, Navbar as BsNavbar, Nav, Button } from 'react-bootstrap';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const dashboardLink = () => {
    if (!user) return null;
    switch (user.role) {
      case 'STUDENT':
        return '/student/applications';
      case 'COMPANY':
        return '/company/dashboard';
      case 'UNIVERSITY_ADMIN':
        return '/university-admin/dashboard';
      case 'ADMIN':
        return '/admin/dashboard';
      default:
        return '/';
    }
  };

  return (
    <BsNavbar expand="lg" className="glass-nav mb-0" variant="dark">
      <Container>
        <BsNavbar.Brand as={Link} to="/">
          INTERN.PRO
        </BsNavbar.Brand>
        <BsNavbar.Toggle aria-controls="nav" />
        <BsNavbar.Collapse id="nav">
          <Nav className="ms-auto align-items-center gap-1">
            <Nav.Link as={Link} to="/verify">
              <i className="bi bi-patch-check me-1" />
              Проверка сертификата
            </Nav.Link>
            {user ? (
              <>
                <Nav.Link as={Link} to={dashboardLink()!}>
                  <i className="bi bi-grid-1x2 me-1" />
                  Кабинет
                </Nav.Link>
                {user.role === 'STUDENT' && (
                  <Nav.Link as={Link} to="/student/profile">
                    <i className="bi bi-person-circle me-1" />
                    Профиль
                  </Nav.Link>
                )}
                <Button className="btn-nav-danger ms-2" size="sm" onClick={handleLogout}>
                  Выйти
                </Button>
              </>
            ) : (
              <>
                <Nav.Link as={Link} to="/login">
                  Войти
                </Nav.Link>
                <Link to="/register" className="btn btn-nav-outline ms-2">
                  Регистрация
                </Link>
              </>
            )}
          </Nav>
        </BsNavbar.Collapse>
      </Container>
    </BsNavbar>
  );
}

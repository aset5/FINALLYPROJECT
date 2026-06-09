import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import { AuthProvider } from './context/AuthContext';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminApplicationsPage from './pages/admin/ApplicationsPage';
import AdminCertificatesPage from './pages/admin/CertificatesPage';
import AdminDashboardPage from './pages/admin/DashboardPage';
import AdminUsersPage from './pages/admin/UsersPage';
import CompanyChatPage from './pages/company/ChatPage';
import CompanyDashboardPage from './pages/company/DashboardPage';
import CompanyProfilePage from './pages/company/ProfilePage';
import ApplicationsPage from './pages/student/ApplicationsPage';
import StudentChatPage from './pages/student/ChatPage';
import JobMarketPage from './pages/student/JobMarketPage';
import LearningPage from './pages/student/LearningPage';
import ProfilePage from './pages/student/ProfilePage';
import UniversityDashboardPage from './pages/university/DashboardPage';
import VerifyCertificatePage from './pages/VerifyCertificatePage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <main className="app-main">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify" element={<VerifyCertificatePage />} />
          <Route path="/verify/:certNumber" element={<VerifyCertificatePage />} />

          <Route
            path="/student/applications"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <ApplicationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/profile"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/job-market"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <JobMarketPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/chat/:internshipId"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <StudentChatPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/learning/:applicationId"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <LearningPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/company/dashboard"
            element={
              <ProtectedRoute roles={['COMPANY']}>
                <CompanyDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/company/profile"
            element={
              <ProtectedRoute roles={['COMPANY']}>
                <CompanyProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/company/chat/:internshipId/:studentId"
            element={
              <ProtectedRoute roles={['COMPANY']}>
                <CompanyChatPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/university-admin/dashboard"
            element={
              <ProtectedRoute roles={['UNIVERSITY_ADMIN']}>
                <UniversityDashboardPage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <AdminDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/applications"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <AdminApplicationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/certificates"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <AdminCertificatesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <AdminUsersPage />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </main>
      </BrowserRouter>
    </AuthProvider>
  );
}

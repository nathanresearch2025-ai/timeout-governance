import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import AppLayout from './components/AppLayout';
import PolicyList from './pages/PolicyList';
import PolicyForm from './pages/PolicyForm';
import TimeoutEvents from './pages/TimeoutEvents';

function App() {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#1677ff',
        },
      }}
    >
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            <Route index element={<Navigate to="/policies" replace />} />
            <Route path="policies" element={<PolicyList />} />
            <Route path="policies/create" element={<PolicyForm />} />
            <Route path="policies/:id/edit" element={<PolicyForm />} />
            <Route path="timeout-events" element={<TimeoutEvents />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}

export default App;

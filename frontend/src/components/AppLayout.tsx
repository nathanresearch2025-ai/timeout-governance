import { Layout, Menu } from 'antd';
import { ClockCircleOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';

const { Header, Content, Sider } = Layout;

const menuItems = [
  {
    key: '/policies',
    icon: <SafetyCertificateOutlined />,
    label: 'Timeout Policies',
  },
  {
    key: '/timeout-events',
    icon: <ClockCircleOutlined />,
    label: 'Timeout Events',
  },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const selectedKey = location.pathname.startsWith('/policies')
    ? '/policies'
    : location.pathname;

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', padding: '0 24px' }}>
        <h1 style={{ color: '#fff', margin: 0, fontSize: 18 }}>
          DolphinScheduler Timeout Governance
        </h1>
      </Header>
      <Layout>
        <Sider width={240} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

import { useEffect, useState } from 'react';
import { Table, Button, Space, Tag, Popconfirm, message, Card, Input, Select } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { policyApi } from '../api';
import type { TimeoutPolicy, PolicyLevel, TimeoutAction } from '../types';
import type { ColumnsType } from 'antd/es/table';

const levelColors: Record<PolicyLevel, string> = {
  TEAM: 'blue',
  WORKFLOW: 'green',
  TASK: 'orange',
};

const actionColors: Record<TimeoutAction, string> = {
  ALERT: 'warning',
  KILL: 'error',
  ALERT_AND_KILL: 'volcano',
  ESCALATE: 'purple',
};

export default function PolicyList() {
  const navigate = useNavigate();
  const [data, setData] = useState<TimeoutPolicy[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [teamFilter, setTeamFilter] = useState<string>('');
  const [levelFilter, setLevelFilter] = useState<string>('');

  const fetchData = async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page, size: pageSize };
      if (teamFilter) params.teamId = teamFilter;
      if (levelFilter) params.level = levelFilter;
      const res = await policyApi.list(params as never);
      setData(res.data.content);
      setTotal(res.data.totalElements);
    } catch {
      message.error('Failed to load policies');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [page, pageSize, teamFilter, levelFilter]);

  const handleDelete = async (id: string) => {
    try {
      await policyApi.delete(id);
      message.success('Policy deleted');
      fetchData();
    } catch {
      message.error('Failed to delete policy');
    }
  };

  const columns: ColumnsType<TimeoutPolicy> = [
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: 'Level',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level: PolicyLevel) => <Tag color={levelColors[level]}>{level}</Tag>,
    },
    {
      title: 'Team',
      dataIndex: 'teamId',
      key: 'teamId',
      width: 150,
    },
    {
      title: 'Timeout (min)',
      dataIndex: 'timeoutMinutes',
      key: 'timeoutMinutes',
      width: 120,
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
      width: 140,
      render: (action: TimeoutAction) => <Tag color={actionColors[action]}>{action}</Tag>,
    },
    {
      title: 'Channels',
      dataIndex: 'alertChannels',
      key: 'alertChannels',
      width: 150,
      render: (channels: string) =>
        channels?.split(',').map((ch) => <Tag key={ch}>{ch.trim()}</Tag>),
    },
    {
      title: 'Enabled',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled: boolean) =>
        enabled ? <Tag color="success">Yes</Tag> : <Tag color="default">No</Tag>,
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => navigate(`/policies/${record.id}/edit`)}
          />
          <Popconfirm
            title="Delete this policy?"
            onConfirm={() => handleDelete(record.id!)}
          >
            <Button type="link" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title="Timeout Policies"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/policies/create')}>
          Create Policy
        </Button>
      }
    >
      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="Filter by team"
          value={teamFilter}
          onChange={(e) => { setTeamFilter(e.target.value); setPage(0); }}
          style={{ width: 180 }}
          allowClear
        />
        <Select
          placeholder="Filter by level"
          value={levelFilter || undefined}
          onChange={(val) => { setLevelFilter(val || ''); setPage(0); }}
          style={{ width: 150 }}
          allowClear
          options={[
            { label: 'TEAM', value: 'TEAM' },
            { label: 'WORKFLOW', value: 'WORKFLOW' },
            { label: 'TASK', value: 'TASK' },
          ]}
        />
      </Space>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          onChange: (p, s) => { setPage(p - 1); setPageSize(s); },
          showSizeChanger: true,
          showTotal: (t) => `Total ${t} policies`,
        }}
      />
    </Card>
  );
}

import { useEffect, useState } from 'react';
import { Table, Card, Tag, Space, Badge } from 'antd';
import { timeoutEventApi } from '../api';
import type { TimeoutEvent, PolicyLevel, TimeoutAction } from '../types';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

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

export default function TimeoutEvents() {
  const [data, setData] = useState<TimeoutEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await timeoutEventApi.list({ page, size: pageSize });
      setData(res.data.content);
      setTotal(res.data.totalElements);
    } catch {
      // silently fail
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [page, pageSize]);

  const columns: ColumnsType<TimeoutEvent> = [
    {
      title: 'Status',
      key: 'status',
      width: 100,
      render: (_, record) => {
        if (record.resolvedAt) return <Badge status="success" text="Resolved" />;
        if (record.escalated) return <Badge status="error" text="Escalated" />;
        return <Badge status="processing" text="Active" />;
      },
    },
    {
      title: 'Workflow',
      dataIndex: 'workflowName',
      key: 'workflowName',
      width: 180,
    },
    {
      title: 'Task',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 180,
    },
    {
      title: 'Team',
      dataIndex: 'teamId',
      key: 'teamId',
      width: 140,
    },
    {
      title: 'Policy Level',
      dataIndex: 'policyLevel',
      key: 'policyLevel',
      width: 110,
      render: (level: PolicyLevel) => <Tag color={levelColors[level]}>{level}</Tag>,
    },
    {
      title: 'Duration',
      key: 'duration',
      width: 150,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ color: '#f5222d' }}>{record.actualDurationMinutes} min</span>
          <span style={{ color: '#999', fontSize: 12 }}>limit: {record.timeoutMinutes} min</span>
        </Space>
      ),
    },
    {
      title: 'Action',
      dataIndex: 'actionTaken',
      key: 'actionTaken',
      width: 130,
      render: (action: TimeoutAction) => <Tag color={actionColors[action]}>{action}</Tag>,
    },
    {
      title: 'Detected At',
      dataIndex: 'detectedAt',
      key: 'detectedAt',
      width: 170,
      render: (val: string) => val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: 'Resolved At',
      dataIndex: 'resolvedAt',
      key: 'resolvedAt',
      width: 170,
      render: (val: string | null) => val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
  ];

  return (
    <Card title="Timeout Events">
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
          showTotal: (t) => `Total ${t} events`,
        }}
      />
    </Card>
  );
}

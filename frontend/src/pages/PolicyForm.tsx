import { useEffect, useState } from 'react';
import { Form, Input, Select, InputNumber, Switch, Button, Card, message, Space } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { policyApi } from '../api';
import type { TimeoutPolicy } from '../types';

export default function PolicyForm() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!id;

  useEffect(() => {
    if (isEdit) {
      policyApi.getById(id).then((res) => {
        const data = res.data;
        form.setFieldsValue({
          ...data,
          alertChannels: data.alertChannels ? data.alertChannels.split(',') : [],
        });
      });
    }
  }, [id]);

  const onFinish = async (values: TimeoutPolicy) => {
    setLoading(true);
    try {
      const payload = {
        ...values,
        alertChannels: Array.isArray(values.alertChannels)
          ? values.alertChannels.join(',')
          : values.alertChannels,
      };
      if (isEdit) {
        await policyApi.update(id, payload);
        message.success('Policy updated');
      } else {
        await policyApi.create(payload);
        message.success('Policy created');
      }
      navigate('/policies');
    } catch {
      message.error('Failed to save policy');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title={isEdit ? 'Edit Policy' : 'Create Policy'} style={{ maxWidth: 700 }}>
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{ enabled: true, timeoutMinutes: 60, escalationMinutes: 30 }}
      >
        <Form.Item name="name" label="Policy Name" rules={[{ required: true }]}>
          <Input placeholder="e.g. Data Engineering Team Default" />
        </Form.Item>

        <Form.Item name="level" label="Policy Level" rules={[{ required: true }]}>
          <Select
            options={[
              { label: 'TEAM - Apply to all tasks in a team', value: 'TEAM' },
              { label: 'WORKFLOW - Apply to a specific workflow', value: 'WORKFLOW' },
              { label: 'TASK - Apply to a specific task', value: 'TASK' },
            ]}
          />
        </Form.Item>

        <Form.Item name="teamId" label="Team ID" rules={[{ required: true }]}>
          <Input placeholder="e.g. data-engineering" />
        </Form.Item>

        <Form.Item name="targetId" label="Target ID (Workflow/Task ID)">
          <Input placeholder="Leave empty for TEAM level policies" />
        </Form.Item>

        <Space size="large">
          <Form.Item name="timeoutMinutes" label="Timeout (minutes)" rules={[{ required: true }]}>
            <InputNumber min={1} max={1440} />
          </Form.Item>

          <Form.Item name="escalationMinutes" label="Escalation After (minutes)" rules={[{ required: true }]}>
            <InputNumber min={1} max={1440} />
          </Form.Item>
        </Space>

        <Form.Item name="action" label="Timeout Action" rules={[{ required: true }]}>
          <Select
            options={[
              { label: 'ALERT - Send notification only', value: 'ALERT' },
              { label: 'KILL - Kill the task', value: 'KILL' },
              { label: 'ALERT_AND_KILL - Alert then kill', value: 'ALERT_AND_KILL' },
              { label: 'ESCALATE - Escalate immediately', value: 'ESCALATE' },
            ]}
          />
        </Form.Item>

        <Form.Item name="alertChannels" label="Alert Channels" rules={[{ required: true }]}>
          <Select
            mode="tags"
            placeholder="Select or type channels"
            options={[
              { label: 'Email', value: 'email' },
              { label: 'DingTalk', value: 'dingtalk' },
              { label: 'WeChat', value: 'wechat' },
              { label: 'WhatsApp', value: 'whatsapp' },
            ]}
          />
        </Form.Item>

        <Form.Item name="escalationContacts" label="Escalation Contacts">
          <Input placeholder="e.g. oncall@company.com" />
        </Form.Item>

        <Form.Item name="enabled" label="Enabled" valuePropName="checked">
          <Switch />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              {isEdit ? 'Update' : 'Create'}
            </Button>
            <Button onClick={() => navigate('/policies')}>Cancel</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
}

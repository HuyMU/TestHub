import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  Row,
  Col,
  Tag,
  Button,
  Table,
  Space,
  Typography,
  Popconfirm,
  message,
  Statistic,
  Timeline,
  Tooltip,
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  MinusCircleOutlined,
  ClockCircleOutlined,
  LockOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  CheckOutlined,
  SyncOutlined,
  HistoryOutlined,
  FileOutlined,
} from '@ant-design/icons';
import { PageHeader } from '../../components/PageHeader';
import { TestRunCase } from '../../types';
import { useAuthStore } from '../../store/authStore';
import { useTestRunStore } from './useTestRunStore';
import * as testRunApi from './testRunApi';
import * as executionApi from '../execution/executionApi';
import { ExecuteResultModal } from '../execution/ExecuteResultModal';
import { ReviewResultModal } from '../execution/ReviewResultModal';

const { Text, Paragraph } = Typography;

export const TestRunDetailPage: React.FC = () => {
  const { runId } = useParams<{ runId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';

  const { currentRun, loading, fetchRunDetail } = useTestRunStore();
  const [closing, setClosing] = useState(false);

  // Execution modal state
  const [executeModalOpen, setExecuteModalOpen] = useState(false);
  const [selectedCaseForExec, setSelectedCaseForExec] = useState<TestRunCase | null>(null);

  // Retest review modal state
  const [retestModalOpen, setRetestModalOpen] = useState(false);
  const [selectedCaseForRetest, setSelectedCaseForRetest] = useState<TestRunCase | null>(null);

  // Execution history state per caseId
  const [histories, setHistories] = useState<Record<number, executionApi.ExecutionHistoryItem[]>>({});
  const [loadingHistory, setLoadingHistory] = useState<Record<number, boolean>>({});

  useEffect(() => {
    if (runId) {
      fetchRunDetail(Number(runId));
    }
  }, [runId]);

  const handleCloseRun = async () => {
    if (!currentRun) return;
    setClosing(true);
    try {
      await testRunApi.closeTestRun(currentRun.id);
      message.success('Test Run closed');
      fetchRunDetail(currentRun.id);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to close Test Run');
    } finally {
      setClosing(false);
    }
  };

  const handleRemoveCase = async (runCaseId: number) => {
    if (!currentRun) return;
    try {
      await testRunApi.removeCaseFromRun(currentRun.id, runCaseId);
      message.success('Case removed from Test Run');
      fetchRunDetail(currentRun.id);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to remove case');
    }
  };

  const handleOpenExecute = (record: TestRunCase) => {
    setSelectedCaseForExec(record);
    setExecuteModalOpen(true);
  };

  const handleLeaderApprove = async (record: TestRunCase) => {
    if (!currentRun) return;
    try {
      await executionApi.reviewResult(currentRun.id, record.caseId, true);
      message.success('Execution result marked as Reviewed');
      fetchRunDetail(currentRun.id);
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to approve review');
    }
  };

  const handleOpenRetestModal = (record: TestRunCase) => {
    setSelectedCaseForRetest(record);
    setRetestModalOpen(true);
  };

  const fetchCaseHistory = async (caseId: number) => {
    if (!currentRun) return;
    setLoadingHistory((prev) => ({ ...prev, [caseId]: true }));
    try {
      const historyList = await executionApi.getExecutionHistory(currentRun.id, caseId);
      setHistories((prev) => ({ ...prev, [caseId]: historyList }));
    } catch (err: any) {
      // Ignore background error
    } finally {
      setLoadingHistory((prev) => ({ ...prev, [caseId]: false }));
    }
  };

  if (!currentRun && !loading) {
    return (
      <Card style={{ marginTop: 16, textAlign: 'center' }}>
        <Text type="secondary">Test Run not found.</Text>
      </Card>
    );
  }

  const columns = [
    {
      title: 'Code',
      dataIndex: 'code',
      key: 'code',
      width: 90,
      render: (val: string) => <Text strong>{val}</Text>,
    },
    {
      title: 'Title (Snapshot)',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
    },
    {
      title: 'Assigned To',
      dataIndex: 'assignedToName',
      key: 'assignedToName',
      width: 140,
      render: (val?: string) => val || <Text type="secondary">Unassigned</Text>,
    },
    {
      title: 'Result Status',
      dataIndex: 'resultStatus',
      key: 'resultStatus',
      width: 180,
      render: (val: string, record: TestRunCase) => {
        let tag = <Tag color="default" icon={<ClockCircleOutlined />}>Untested</Tag>;
        switch (val) {
          case 'PASSED':
            tag = <Tag color="green" icon={<CheckCircleOutlined />}>Passed</Tag>;
            break;
          case 'FAILED':
            tag = <Tag color="red" icon={<CloseCircleOutlined />}>Failed</Tag>;
            break;
          case 'BLOCKED':
            tag = <Tag color="orange" icon={<MinusCircleOutlined />}>Blocked</Tag>;
            break;
          case 'RETEST':
            tag = <Tag color="purple" icon={<SyncOutlined />}>Retest</Tag>;
            break;
        }

        return (
          <Space>
            {tag}
            {record.isReviewed && <Tag color="blue">Reviewed</Tag>}
          </Space>
        );
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 260,
      render: (_: any, record: TestRunCase) => {
        const isOpen = currentRun?.status === 'OPEN';
        const isAssigned = record.assignedToId === user?.id;
        const canExecute = isOpen && (isLeader || isAssigned);
        const canReview = isLeader && record.resultStatus !== 'UNTESTED';

        return (
          <Space size="small">
            {canExecute && (
              <Button
                type="primary"
                size="small"
                icon={<PlayCircleOutlined />}
                onClick={() => handleOpenExecute(record)}
              >
                Execute
              </Button>
            )}

            {canReview && (
              <>
                <Tooltip title="Mark Result as Reviewed">
                  <Button
                    type="default"
                    size="small"
                    icon={<CheckOutlined />}
                    onClick={() => handleLeaderApprove(record)}
                  >
                    Approve
                  </Button>
                </Tooltip>
                <Tooltip title="Request Retest (requires comment)">
                  <Button
                    danger
                    size="small"
                    icon={<SyncOutlined />}
                    onClick={() => handleOpenRetestModal(record)}
                  >
                    Retest
                  </Button>
                </Tooltip>
              </>
            )}

            {isLeader && isOpen && (
              <Popconfirm
                title="Remove Case"
                description="Remove this case from Test Run?"
                onConfirm={() => handleRemoveCase(record.id)}
                okText="Remove"
                cancelText="Cancel"
              >
                <Button type="text" size="small" danger icon={<DeleteOutlined />} title="Remove Case" />
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <PageHeader
        title={
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
            <span>{currentRun?.name || 'Test Run Details'}</span>
            {currentRun?.status === 'OPEN' ? (
              <Tag color="green">Open</Tag>
            ) : (
              <Tag color="default" icon={<LockOutlined />}>Closed</Tag>
            )}
          </Space>
        }
        extra={
          isLeader && currentRun?.status === 'OPEN' ? (
            <Popconfirm
              title="Close Test Run"
              description="Are you sure you want to close this Test Run? No further case modifications will be allowed."
              onConfirm={handleCloseRun}
              okText="Close Run"
              cancelText="Cancel"
            >
              <Button type="primary" danger icon={<LockOutlined />} loading={closing}>
                Close Run
              </Button>
            </Popconfirm>
          ) : null
        }
      />

      {/* Metrics Header Cards */}
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Total Cases" value={currentRun?.totalCases || 0} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Passed" value={currentRun?.passedCases || 0} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Failed" value={currentRun?.failedCases || 0} valueStyle={{ color: '#ff4d4f' }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Blocked" value={currentRun?.blockedCases || 0} valueStyle={{ color: '#fa8c16' }} />
          </Card>
        </Col>
        <Col xs={12} sm={8} md={4}>
          <Card size="small">
            <Statistic title="Untested" value={currentRun?.untestedCases || 0} />
          </Card>
        </Col>
      </Row>

      {/* Case Snapshot Table */}
      <Card
        title="Snapshotted Test Cases"
        style={{ marginTop: 16 }}
        loading={loading}
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={currentRun?.cases || []}
          pagination={{ pageSize: 10 }}
          expandable={{
            onExpand: (expanded, record) => {
              if (expanded) {
                fetchCaseHistory(record.caseId);
              }
            },
            expandedRowRender: (record) => {
              const historyList = histories[record.caseId] || [];
              const isHistLoading = loadingHistory[record.caseId];

              return (
                <div style={{ padding: '12px 20px', background: '#fafafa', borderRadius: 6 }}>
                  <Paragraph><strong>Precondition:</strong> {record.precondition || 'None'}</Paragraph>
                  <Paragraph><strong>Steps:</strong></Paragraph>
                  <pre style={{ background: '#fff', padding: 8, borderRadius: 4 }}>{record.steps}</pre>
                  <Paragraph><strong>Expected Result:</strong></Paragraph>
                  <pre style={{ background: '#fff', padding: 8, borderRadius: 4 }}>{record.expectedResult}</pre>
                  {record.testData && (
                    <Paragraph><strong>Test Data:</strong> {record.testData}</Paragraph>
                  )}
                  {record.defectRef && (
                    <Paragraph><strong>Defect Ref:</strong> <Tag color="volcano">{record.defectRef}</Tag></Paragraph>
                  )}

                  <div style={{ marginTop: 16 }}>
                    <Space style={{ marginBottom: 12 }}>
                      <HistoryOutlined style={{ color: '#1890ff' }} />
                      <Text strong>Execution History Logs</Text>
                    </Space>

                    {isHistLoading ? (
                      <div>Loading execution history...</div>
                    ) : historyList.length > 0 ? (
                      <Timeline
                        mode="left"
                        style={{ marginTop: 8 }}
                        items={historyList.map((item) => ({
                          color:
                            item.resultStatus === 'PASSED'
                              ? 'green'
                              : item.resultStatus === 'FAILED'
                              ? 'red'
                              : item.resultStatus === 'BLOCKED'
                              ? 'orange'
                              : 'purple',
                          children: (
                            <div>
                              <Space>
                                <Tag color={item.resultStatus === 'PASSED' ? 'green' : item.resultStatus === 'FAILED' ? 'red' : 'orange'}>
                                  {item.resultStatus}
                                </Tag>
                                <Text type="secondary">{item.executedBy || 'Unknown'} at {new Date(item.executedAt).toLocaleString()}</Text>
                              </Space>
                              {item.comment && (
                                <p style={{ margin: '4px 0 0 0', color: '#595959' }}>{item.comment}</p>
                              )}
                            </div>
                          ),
                        }))}
                      />
                    ) : (
                      <Paragraph type="secondary">No execution history recorded yet.</Paragraph>
                    )}
                  </div>
                </div>
              );
            },
          }}
        />
      </Card>

      {/* Execution Modal */}
      {selectedCaseForExec && (
        <ExecuteResultModal
          runId={currentRun?.id || 0}
          runCase={selectedCaseForExec}
          open={executeModalOpen}
          onCancel={() => {
            setExecuteModalOpen(false);
            setSelectedCaseForExec(null);
          }}
          onSuccess={() => {
            if (currentRun) fetchRunDetail(currentRun.id);
          }}
        />
      )}

      {/* Retest Review Modal */}
      {selectedCaseForRetest && (
        <ReviewResultModal
          runId={currentRun?.id || 0}
          runCase={selectedCaseForRetest}
          open={retestModalOpen}
          onCancel={() => {
            setRetestModalOpen(false);
            setSelectedCaseForRetest(null);
          }}
          onSuccess={() => {
            if (currentRun) fetchRunDetail(currentRun.id);
          }}
        />
      )}
    </div>
  );
};

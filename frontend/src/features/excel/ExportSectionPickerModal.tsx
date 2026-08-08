import React, { useState } from 'react';
import { Modal, Tree, Button, Space, Typography, message } from 'antd';
import { DownloadOutlined, FolderOutlined } from '@ant-design/icons';
import { Section } from '../../types';
import * as excelApi from './excelApi';

const { Text, Paragraph } = Typography;

interface ExportSectionPickerModalProps {
  projectId: number;
  open: boolean;
  sections: Section[];
  onCancel: () => void;
}

export const ExportSectionPickerModal: React.FC<ExportSectionPickerModalProps> = ({
  projectId,
  open,
  sections,
  onCancel,
}) => {
  const [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
  const [exporting, setExporting] = useState(false);

  // Map Sections to TreeData format
  const mapSectionToTreeNode = (node: Section): any => ({
    title: node.name,
    key: node.id,
    children: node.children ? node.children.map(mapSectionToTreeNode) : [],
  });

  const treeData = sections.map(mapSectionToTreeNode);

  const handleExport = async () => {
    setExporting(true);
    try {
      const sectionIds = checkedKeys.map((k) => Number(k));
      await excelApi.exportCases(projectId, sectionIds.length > 0 ? sectionIds : undefined);
      message.success('Export downloaded successfully');
      onCancel();
    } catch (err: any) {
      message.error(err?.response?.data?.message || 'Failed to export test cases');
    } finally {
      setExporting(false);
    }
  };

  return (
    <Modal
      title={
        <Space>
          <DownloadOutlined style={{ color: '#1890ff' }} />
          <span>Export Test Cases to Excel</span>
        </Space>
      }
      open={open}
      onCancel={onCancel}
      footer={
        <Space>
          <Button onClick={onCancel}>Cancel</Button>
          <Button
            type="primary"
            icon={<DownloadOutlined />}
            onClick={handleExport}
            loading={exporting}
          >
            {checkedKeys.length > 0 ? `Export Selected (${checkedKeys.length})` : 'Export Entire Project'}
          </Button>
        </Space>
      }
      destroyOnClose
    >
      <Paragraph type="secondary">
        Select specific sections/subsections to export, or leave blank to export all test cases in the project. Each root section will be formatted into a separate sheet.
      </Paragraph>

      <div style={{ maxHeight: 300, overflowY: 'auto', border: '1px solid #f0f0f0', padding: 12, borderRadius: 6 }}>
        {treeData.length === 0 ? (
          <Text type="secondary">No sections available to export.</Text>
        ) : (
          <Tree
            checkable
            treeData={treeData}
            checkedKeys={checkedKeys}
            onCheck={(keys) => setCheckedKeys(Array.isArray(keys) ? keys : keys.checked)}
            defaultExpandAll
          />
        )}
      </div>
    </Modal>
  );
};

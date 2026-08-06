import React from 'react';
import { Modal, Upload, Button } from 'antd';
import { UploadOutlined } from '@ant-design/icons';

interface ImportModalProps {
  open: boolean;
  onClose: () => void;
}

export const ImportModal: React.FC<ImportModalProps> = ({ open, onClose }) => {
  return (
    <Modal title="Import Test Cases (2-Step Validation)" open={open} onCancel={onClose} footer={null}>
      <Upload.Dragger accept=".xlsx, .xls">
        <p className="ant-upload-drag-icon">
          <UploadOutlined />
        </p>
        <p className="ant-upload-text">Click or drag Excel template file to this area to validate</p>
      </Upload.Dragger>
      <div style={{ marginTop: 16, textAlign: 'right' }}>
        <Button onClick={onClose}>Cancel</Button>
      </div>
    </Modal>
  );
};

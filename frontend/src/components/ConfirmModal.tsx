import React from 'react';
import { Modal } from 'antd';

interface ConfirmModalProps {
  visible: boolean;
  title: string;
  content: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  visible,
  title,
  content,
  onConfirm,
  onCancel,
}) => {
  return (
    <Modal open={visible} title={title} onOk={onConfirm} onCancel={onCancel}>
      <p>{content}</p>
    </Modal>
  );
};

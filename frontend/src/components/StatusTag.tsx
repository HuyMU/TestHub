import React from 'react';
import { Tag } from 'antd';

interface StatusTagProps {
  type: 'caseStatus' | 'resultStatus' | 'priority';
  value: string;
}

export const StatusTag: React.FC<StatusTagProps> = ({ type, value }) => {
  let color = 'default';
  const valUpper = (value || '').toUpperCase();

  if (type === 'caseStatus') {
    switch (valUpper) {
      case 'DRAFT':
        color = 'processing';
        break;
      case 'REVIEW':
        color = 'warning';
        break;
      case 'READY':
        color = 'success';
        break;
    }
  } else if (type === 'resultStatus') {
    switch (valUpper) {
      case 'PASSED':
        color = 'success';
        break;
      case 'FAILED':
        color = 'error';
        break;
      case 'BLOCKED':
        color = 'warning';
        break;
      case 'RETEST':
        color = 'purple';
        break;
      default:
        color = 'default';
    }
  } else if (type === 'priority') {
    switch (valUpper) {
      case 'CRITICAL':
        color = 'red';
        break;
      case 'HIGH':
        color = 'orange';
        break;
      case 'MEDIUM':
        color = 'blue';
        break;
      case 'LOW':
        color = 'default';
        break;
    }
  }

  return <Tag color={color}>{value}</Tag>;
};

import React from 'react';
import { Tag } from 'antd';
import { CaseStatus, ResultStatus, Priority } from '../types';

interface StatusTagProps {
  type: 'caseStatus' | 'resultStatus' | 'priority';
  value: string;
}

export const StatusTag: React.FC<StatusTagProps> = ({ type, value }) => {
  let color = 'default';

  if (type === 'caseStatus') {
    switch (value as CaseStatus) {
      case 'Draft':
        color = 'orange';
        break;
      case 'Review':
        color = 'blue';
        break;
      case 'Ready':
        color = 'green';
        break;
    }
  } else if (type === 'resultStatus') {
    switch (value as ResultStatus) {
      case 'Passed':
        color = 'success';
        break;
      case 'Failed':
        color = 'error';
        break;
      case 'Blocked':
        color = 'warning';
        break;
      case 'Retest':
        color = 'purple';
        break;
      default:
        color = 'default';
    }
  } else if (type === 'priority') {
    switch (value as Priority) {
      case 'Critical':
        color = 'magenta';
        break;
      case 'High':
        color = 'red';
        break;
      case 'Medium':
        color = 'volcano';
        break;
      case 'Low':
        color = 'cyan';
        break;
    }
  }

  return <Tag color={color}>{value}</Tag>;
};

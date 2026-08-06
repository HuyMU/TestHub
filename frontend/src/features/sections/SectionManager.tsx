import React from 'react';
import { Card } from 'antd';
import { SectionTree } from '../../components/SectionTree';

export const SectionManager: React.FC = () => {
  return (
    <Card title="Section / Subsection Hierarchy">
      <SectionTree sections={[]} />
    </Card>
  );
};

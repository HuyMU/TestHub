import React from 'react';
import { Tree } from 'antd';
import { Section } from '../types';

interface SectionTreeProps {
  sections: Section[];
  onSelect?: (selectedKeys: React.Key[]) => void;
}

export const SectionTree: React.FC<SectionTreeProps> = ({ sections, onSelect }) => {
  const mapSectionsToTree = (items: Section[]): any[] => {
    return items.map((item) => ({
      title: item.name,
      key: item.id.toString(),
      children: item.children ? mapSectionsToTree(item.children) : [],
    }));
  };

  return <Tree treeData={mapSectionsToTree(sections)} onSelect={onSelect} defaultExpandAll />;
};

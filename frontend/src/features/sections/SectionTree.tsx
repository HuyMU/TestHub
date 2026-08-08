import React, { useEffect, useState } from 'react';
import { Tree, Button, Modal, Form, Input, Select, Space, Popconfirm, message, Typography, Card } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, FolderOutlined, FolderOpenOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { Section } from '../../types';
import { useAuthStore } from '../../store/authStore';
import { useSectionStore } from './useSectionStore';
import * as sectionApi from './sectionApi';

const { Text } = Typography;

interface SectionTreeProps {
  projectId: number;
}

export const SectionTree: React.FC<SectionTreeProps> = ({ projectId }) => {
  const { t } = useTranslation();
  const { user } = useAuthStore();
  const isLeader = user?.role === 'LEADER';
  const { sections, selectedSectionId, loading, fetchSections, selectSection, setSections } = useSectionStore();

  // Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [targetSection, setTargetSection] = useState<Section | null>(null);
  const [form] = Form.useForm();
  const [submitLoading, setSubmitLoading] = useState(false);

  useEffect(() => {
    if (projectId) {
      fetchSections(projectId);
    }
  }, [projectId]);

  const openCreateModal = (parent?: Section, e?: React.MouseEvent) => {
    e?.stopPropagation();
    setModalMode('create');
    setTargetSection(parent || null);
    form.resetFields();
    form.setFieldsValue({
      parentSectionId: parent ? parent.id : null,
    });
    setModalOpen(true);
  };

  const openEditModal = (section: Section, e: React.MouseEvent) => {
    e.stopPropagation();
    setModalMode('edit');
    setTargetSection(section);
    form.setFieldsValue({
      name: section.name,
      parentSectionId: section.parentSectionId || null,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitLoading(true);

      if (modalMode === 'create') {
        await sectionApi.createSection(projectId, {
          name: values.name,
          parentSectionId: values.parentSectionId || null,
        });
        message.success('Section created successfully');
      } else if (targetSection) {
        await sectionApi.updateSection(targetSection.id, {
          name: values.name,
          parentSectionId: values.parentSectionId || null,
        });
        message.success('Section updated successfully');
      }

      setModalOpen(false);
      fetchSections(projectId);
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setSubmitLoading(false);
    }
  };

  const handleDelete = async (section: Section, e?: React.MouseEvent) => {
    e?.stopPropagation();
    try {
      await sectionApi.deleteSection(section.id);
      message.success('Section deleted successfully');
      if (selectedSectionId === section.id) {
        selectSection(null);
      }
      fetchSections(projectId);
    } catch (err: any) {
      const status = err?.response?.status;
      const errorMsg = err?.response?.data?.message || 'Failed to delete section';

      if (status === 409) {
        Modal.error({
          title: 'Cannot Delete Section',
          content: errorMsg,
          okText: 'Understood',
        });
      } else {
        message.error(errorMsg);
      }
    }
  };

  // Helper to check if targetId is the node itself or one of its descendants
  const isDescendant = (node: Section, targetId: number): boolean => {
    if (node.id === targetId) return true;
    if (node.children && node.children.length > 0) {
      return node.children.some((child) => isDescendant(child, targetId));
    }
    return false;
  };

  // Client-side drop validation to prevent circular references
  const handleAllowDrop = ({ dropNode, dropPosition }: any): boolean => {
    // dropPosition: -1 (before), 0 (inside), 1 (after)
    return true;
  };

  // Drag and drop reordering handler
  const handleDrop = async (info: any) => {
    const dropKey = info.node.key;
    const dragKey = info.dragNode.key;
    const dropPos = info.node.pos.split('-');
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);

    const dragSection: Section = info.dragNode.data;
    const dropSection: Section = info.node.data;

    // Prevent dropping onto self or descendant
    if (isDescendant(dragSection, dropSection.id)) {
      message.error('Cannot move a section into itself or one of its descendants');
      return;
    }

    // Backup current state for rollback
    const originalSections = sections;

    // Determine new parent ID
    let newParentSectionId: number | null = null;
    if (!info.dropToGap) {
      // Dropped directly onto dropNode -> becomes child of dropNode
      newParentSectionId = dropSection.id;
    } else {
      // Dropped to gap before/after dropNode -> shares same parent as dropNode
      newParentSectionId = dropSection.parentSectionId || null;
    }

    // Helper to remove section from tree
    const removeNode = (list: Section[], id: number): { list: Section[]; removed: Section | null } => {
      let removed: Section | null = null;
      const newList = list.filter((item) => {
        if (item.id === id) {
          removed = item;
          return false;
        }
        if (item.children && item.children.length > 0) {
          const res = removeNode(item.children, id);
          if (res.removed) removed = res.removed;
          item.children = res.list;
        }
        return true;
      });
      return { list: newList, removed };
    };

    // Helper to insert section into tree at target parent
    const insertNode = (
      list: Section[],
      nodeToInsert: Section,
      parentId: number | null,
      targetId: number,
      pos: number
    ): Section[] => {
      if (parentId === null) {
        const index = list.findIndex((item) => item.id === targetId);
        const insertIdx = pos < 0 ? index : index + 1;
        const result = [...list];
        result.splice(insertIdx < 0 ? result.length : insertIdx, 0, nodeToInsert);
        return result;
      }
      return list.map((item) => {
        if (item.id === parentId) {
          const children = item.children ? [...item.children] : [];
          if (info.dropToGap) {
            const index = children.findIndex((c) => c.id === targetId);
            const insertIdx = pos < 0 ? index : index + 1;
            children.splice(insertIdx < 0 ? children.length : insertIdx, 0, nodeToInsert);
          } else {
            children.push(nodeToInsert);
          }
          return { ...item, children };
        }
        if (item.children && item.children.length > 0) {
          return { ...item, children: insertNode(item.children, nodeToInsert, parentId, targetId, pos) };
        }
        return item;
      });
    };

    // Clone tree
    const treeCopy = JSON.parse(JSON.stringify(sections));
    const { list: cleanTree, removed: extractedNode } = removeNode(treeCopy, dragKey);

    if (!extractedNode) return;
    extractedNode.parentSectionId = newParentSectionId;

    let updatedTree: Section[] = [];
    if (!info.dropToGap) {
      // Append to children of dropSection
      const addToParent = (list: Section[]): Section[] => {
        return list.map((item) => {
          if (item.id === dropSection.id) {
            const children = item.children ? [...item.children, extractedNode] : [extractedNode];
            return { ...item, children };
          }
          if (item.children && item.children.length > 0) {
            return { ...item, children: addToParent(item.children) };
          }
          return item;
        });
      };
      updatedTree = addToParent(cleanTree);
    } else {
      updatedTree = insertNode(cleanTree, extractedNode, newParentSectionId, dropSection.id, dropPosition);
    }

    // Collect all reorder items for backend payload
    const reorderItems: Array<{ sectionId: number; sortOrder: number; parentSectionId: number | null }> = [];
    const collectReorderItems = (list: Section[], parentId: number | null) => {
      list.forEach((item, index) => {
        item.sortOrder = index;
        item.parentSectionId = parentId;
        reorderItems.push({
          sectionId: item.id,
          sortOrder: index,
          parentSectionId: parentId,
        });
        if (item.children && item.children.length > 0) {
          collectReorderItems(item.children, item.id);
        }
      });
    };
    collectReorderItems(updatedTree, null);

    // Optimistic UI update
    setSections(updatedTree);

    // Backend sync
    try {
      await sectionApi.reorderSections(projectId, reorderItems);
      message.success('Sections reordered');
    } catch (err: any) {
      // Rollback on error
      setSections(originalSections);
      message.error(err?.response?.data?.message || 'Failed to reorder sections');
    }
  };

  // Convert Section tree array into Ant Design TreeData format
  const mapSectionToTreeNode = (node: Section): any => ({
    title: (
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          width: '100%',
          paddingRight: 8,
        }}
      >
        <Space>
          <FolderOutlined />
          <Text strong={selectedSectionId === node.id}>{node.name}</Text>
          {node.directTestCaseCount !== undefined && node.directTestCaseCount > 0 && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              ({node.directTestCaseCount} cases)
            </Text>
          )}
        </Space>
        <Space size="small" onClick={(e) => e.stopPropagation()}>
          <Button
            type="text"
            size="small"
            icon={<PlusOutlined />}
            title="Add Subsection"
            onClick={(e) => openCreateModal(node, e)}
          />
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            title="Edit Section"
            onClick={(e) => openEditModal(node, e)}
          />
          {isLeader && (
            <Popconfirm
              title="Delete Section"
              description="Are you sure you want to delete this section?"
              onConfirm={(e) => handleDelete(node, e)}
              okText="Delete"
              cancelText="Cancel"
            >
              <Button
                type="text"
                size="small"
                danger
                icon={<DeleteOutlined />}
                title="Delete Section (Leader only)"
              />
            </Popconfirm>
          )}
        </Space>
      </div>
    ),
    key: node.id,
    data: node,
    children: node.children ? node.children.map(mapSectionToTreeNode) : [],
  });

  const treeData = sections.map(mapSectionToTreeNode);

  // Flatten sections list for parent selector in modal
  const flattenSections = (list: Section[], depth = 0): Array<{ id: number; name: string }> => {
    let result: Array<{ id: number; name: string }> = [];
    for (const item of list) {
      // Exclude self and self's children when editing
      if (modalMode === 'edit' && targetSection && item.id === targetSection.id) {
        continue;
      }
      const prefix = '- '.repeat(depth);
      result.push({ id: item.id, name: `${prefix}${item.name}` });
      if (item.children && item.children.length > 0) {
        result = result.concat(flattenSections(item.children, depth + 1));
      }
    }
    return result;
  };

  const parentOptions = flattenSections(sections);

  return (
    <Card
      loading={loading}
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Space>
            <FolderOpenOutlined />
            <span>Sections & Subsections</span>
          </Space>
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={(e) => openCreateModal(undefined, e)}
          >
            Add Section
          </Button>
        </div>
      }
    >
      {treeData.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <Text type="secondary">No sections created yet. Click "Add Section" to create one.</Text>
        </div>
      ) : (
        <Tree
          treeData={treeData}
          defaultExpandAll
          draggable
          allowDrop={handleAllowDrop}
          onDrop={handleDrop}
          onSelect={(selectedKeys) => {
            if (selectedKeys.length > 0) {
              selectSection(Number(selectedKeys[0]));
            } else {
              selectSection(null);
            }
          }}
          selectedKeys={selectedSectionId ? [selectedSectionId] : []}
          style={{ background: 'transparent' }}
        />
      )}

      {/* Create / Edit Section Modal */}
      <Modal
        title={modalMode === 'create' ? (targetSection ? `Add Subsection under "${targetSection.name}"` : 'Add Root Section') : `Edit Section "${targetSection?.name}"`}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitLoading}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Section Name"
            rules={[
              { required: true, message: 'Please enter section name' },
              { max: 255, message: 'Name must not exceed 255 characters' },
            ]}
          >
            <Input placeholder="e.g. Authentication, User Profile, Negative Tests" />
          </Form.Item>
          <Form.Item name="parentSectionId" label="Parent Section (Optional)">
            <Select allowClear placeholder="None (Root Section)">
              {parentOptions.map((opt) => (
                <Select.Option key={opt.id} value={opt.id}>
                  {opt.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

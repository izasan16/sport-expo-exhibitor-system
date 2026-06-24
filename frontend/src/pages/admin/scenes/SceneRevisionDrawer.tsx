import { Button, Drawer, List, Space, Tag, Typography } from 'antd';
import type { SceneRevisionSummary } from '../../../app/types/scene';

type SceneRevisionDrawerProps = {
  open: boolean;
  revisions: SceneRevisionSummary[];
  loading?: boolean;
  onClose: () => void;
  onPreviewRevision?: (revisionId: string) => void;
};

export default function SceneRevisionDrawer(props: SceneRevisionDrawerProps) {
  return (
    <Drawer open={props.open} title="发布历史" onClose={props.onClose} width={420}>
      <List
        loading={props.loading}
        dataSource={props.revisions}
        locale={{ emptyText: '暂无发布历史' }}
        renderItem={(item) => (
          <List.Item
            actions={props.onPreviewRevision ? [
              <Button key="preview" type="link" onClick={() => props.onPreviewRevision?.(item.revisionId)}>
                查看
              </Button>,
            ] : undefined}
          >
            <List.Item.Meta
              title={(
                <Space>
                  <Typography.Text>版本 {item.revisionNo}</Typography.Text>
                  <Tag color={item.status === 'PUBLISHED' ? 'green' : 'default'}>{item.status}</Tag>
                </Space>
              )}
              description={item.changeSummary || '无变更说明'}
            />
          </List.Item>
        )}
      />
    </Drawer>
  );
}

import { Empty, Spin } from 'antd';
import { useEffect, useState } from 'react';
import { getPublishedScene, getPublishedSceneBooths } from '../../../app/sceneApi';
import type { PublishedSceneBooth, PublishedSceneResponse } from '../../../app/types/scene';
import SceneSelectionMap from '../../exhibitor/selections/SceneSelectionMap';
import { mapReviewHighlightBooths } from '../../exhibitor/selections/sceneSelectionViewModels';

type SelectionScenePreviewProps = {
  eventId: string;
  highlightedIds: string[];
};

export default function SelectionScenePreview(props: SelectionScenePreviewProps) {
  const [booths, setBooths] = useState<PublishedSceneBooth[]>([]);
  const [publishedScene, setPublishedScene] = useState<PublishedSceneResponse>();
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let alive = true;
    async function load() {
      setLoading(true);
      setFailed(false);
      try {
        const [loadedScene, loadedBooths] = await Promise.all([
          getPublishedScene(props.eventId),
          getPublishedSceneBooths(props.eventId),
        ]);
        if (alive) {
          setPublishedScene(loadedScene);
          setBooths(loadedBooths);
        }
      } catch {
        if (alive) {
          setFailed(true);
          setPublishedScene(undefined);
          setBooths([]);
        }
      } finally {
        if (alive) setLoading(false);
      }
    }
    void load();
    return () => {
      alive = false;
    };
  }, [props.eventId]);

  return (
    <>
      {loading ? <Spin /> : null}
      {!loading && failed ? <Empty description="馆图预览加载失败" /> : null}
      {!loading && !failed ? (
        <SceneSelectionMap
          booths={mapReviewHighlightBooths(booths, props.highlightedIds)}
          onToggle={() => undefined}
          readOnly
          framed={false}
          title="已选展位高亮"
          showViewportControls
          backgroundUrl={publishedScene?.background?.url ?? null}
          canvasWidth={publishedScene?.canvasWidth}
          canvasHeight={publishedScene?.canvasHeight}
        />
      ) : null}
    </>
  );
}

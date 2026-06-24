import type { PublishedSceneBooth, SceneSelectableBooth } from '../../../app/types/scene';

export function mapSelectableSceneBooths(
  booths: PublishedSceneBooth[],
  order: { boothType: string },
  selectedBoothIds: string[],
): SceneSelectableBooth[] {
  return booths
    .filter((item) => item.boothType === order.boothType)
    .map((item) => ({
      ...item,
      selected: selectedBoothIds.includes(item.boothId),
      disabled: item.status !== 'AVAILABLE',
      highlighted: false,
    }));
}

export function mapReviewHighlightBooths(
  booths: PublishedSceneBooth[],
  highlightedIds: string[],
): SceneSelectableBooth[] {
  return booths.map((item) => ({
    ...item,
    selected: highlightedIds.includes(item.boothId),
    highlighted: highlightedIds.includes(item.boothId),
    disabled: true,
  }));
}

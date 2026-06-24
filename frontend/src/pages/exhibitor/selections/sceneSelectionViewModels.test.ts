import { describe, expect, it } from 'vitest';
import { mapReviewHighlightBooths, mapSelectableSceneBooths } from './sceneSelectionViewModels';

describe('sceneSelectionViewModels', () => {
  it('marks selected booth ids and filters out mismatched booth type', () => {
    const result = mapSelectableSceneBooths(
      [
        {
          boothId: 'b1',
          boothNo: 'A01',
          boothType: 'STANDARD',
          status: 'AVAILABLE',
          areaSqm: '9.00',
          geometry: { type: 'Polygon', coordinates: [[[0, 0], [10, 0], [10, 10], [0, 10], [0, 0]]] },
        },
        {
          boothId: 'b2',
          boothNo: 'B01',
          boothType: 'CUSTOM',
          status: 'AVAILABLE',
          areaSqm: '9.00',
          geometry: { type: 'Polygon', coordinates: [[[20, 0], [30, 0], [30, 10], [20, 10], [20, 0]]] },
        },
      ],
      { boothType: 'STANDARD' },
      ['b1'],
    );

    expect(result).toHaveLength(1);
    expect(result[0].selected).toBe(true);
  });

  it('marks reviewed selection booths for admin preview', () => {
    const preview = mapReviewHighlightBooths(
      [
        {
          boothId: 'b1',
          boothNo: 'A01',
          boothType: 'STANDARD',
          status: 'LOCKED',
          areaSqm: '9.00',
          geometry: { type: 'Polygon', coordinates: [[[0, 0], [10, 0], [10, 10], [0, 10], [0, 0]]] },
        },
        {
          boothId: 'b2',
          boothNo: 'A02',
          boothType: 'STANDARD',
          status: 'AVAILABLE',
          areaSqm: '9.00',
          geometry: { type: 'Polygon', coordinates: [[[12, 0], [22, 0], [22, 10], [12, 10], [12, 0]]] },
        },
      ],
      ['b1'],
    );

    expect(preview.find((item) => item.boothId === 'b1')?.highlighted).toBe(true);
  });
});

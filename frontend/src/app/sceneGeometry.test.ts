import { describe, expect, it } from 'vitest';
import { polygonAreaSqm } from './sceneGeometry';

describe('polygonAreaSqm', () => {
  it('converts pixel area into square meters', () => {
    expect(
      polygonAreaSqm(
        [[0, 0], [20, 0], [20, 20], [0, 20], [0, 0]],
        20,
      ),
    ).toBe(1);
  });
});

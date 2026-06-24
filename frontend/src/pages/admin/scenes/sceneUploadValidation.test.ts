import { describe, expect, it } from 'vitest';
import { MAX_SCENE_BACKGROUND_BYTES, validateSceneBackgroundFile } from './sceneUploadValidation';

function createFile(size: number, name = 'floorplan.png', type = 'image/png') {
  return new File([new Uint8Array(size)], name, { type });
}

describe('sceneUploadValidation', () => {
  it('accepts files at or below 10MB', () => {
    const result = validateSceneBackgroundFile(createFile(MAX_SCENE_BACKGROUND_BYTES));
    expect(result.valid).toBe(true);
  });

  it('rejects files larger than 10MB', () => {
    const result = validateSceneBackgroundFile(createFile(MAX_SCENE_BACKGROUND_BYTES + 1));
    expect(result.valid).toBe(false);
    expect(result.message).toBe('底图文件不能超过 10MB，请压缩后重试');
  });
});

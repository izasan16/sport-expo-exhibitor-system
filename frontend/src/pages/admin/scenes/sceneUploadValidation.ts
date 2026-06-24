export const MAX_SCENE_BACKGROUND_BYTES = 10 * 1024 * 1024;

export function validateSceneBackgroundFile(file: File): { valid: boolean; message?: string } {
  if (file.size > MAX_SCENE_BACKGROUND_BYTES) {
    return {
      valid: false,
      message: '底图文件不能超过 10MB，请压缩后重试',
    };
  }

  return { valid: true };
}

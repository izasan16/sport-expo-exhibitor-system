export const MAX_SCENE_BACKGROUND_BYTES = 10 * 1024 * 1024;

export function validateSceneBackgroundFile(file: File): { valid: boolean; message?: string } {
  const extension = file.name.split('.').pop()?.toLowerCase();
  const supported = new Set(['png', 'jpg', 'jpeg', 'svg', 'pdf']);
  if (!extension || !supported.has(extension)) {
    return { valid: false, message: '仅支持 PNG、JPG、SVG 或 PDF 底图文件' };
  }
  if (file.size === 0) {
    return { valid: false, message: '底图文件不能为空' };
  }
  if (file.size > MAX_SCENE_BACKGROUND_BYTES) {
    return {
      valid: false,
      message: '底图文件不能超过 10MB，请压缩后重试',
    };
  }

  const expectedMime = extension === 'jpg' || extension === 'jpeg' ? 'image/jpeg'
    : extension === 'svg' ? 'image/svg+xml'
      : extension === 'pdf' ? 'application/pdf' : 'image/png';
  if (file.type && file.type !== expectedMime) {
    return { valid: false, message: '文件类型与扩展名不匹配' };
  }

  return { valid: true };
}

export function validateSceneCadFile(file: File): { valid: boolean; message?: string } {
  const extension = file.name.split('.').pop()?.toLowerCase();
  if (!extension || !new Set(['dwg', 'dxf']).has(extension)) {
    return { valid: false, message: '仅支持 DWG 或 DXF 文件' };
  }
  if (file.size === 0) {
    return { valid: false, message: 'CAD 文件不能为空' };
  }
  if (file.size > MAX_SCENE_BACKGROUND_BYTES) {
    return { valid: false, message: 'CAD 文件不能超过 10MB' };
  }
  return { valid: true };
}

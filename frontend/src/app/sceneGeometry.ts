export function polygonAreaSqm(points: Array<[number, number]>, pixelToMeterRatio: number) {
  if (points.length < 4 || pixelToMeterRatio <= 0) {
    return 0;
  }

  let twiceArea = 0;
  for (let index = 0; index < points.length - 1; index += 1) {
    const [x1, y1] = points[index];
    const [x2, y2] = points[index + 1];
    twiceArea += x1 * y2 - x2 * y1;
  }

  const areaPx2 = Math.abs(twiceArea) / 2;
  const areaSqm = areaPx2 / (pixelToMeterRatio * pixelToMeterRatio);
  return Number(areaSqm.toFixed(2));
}

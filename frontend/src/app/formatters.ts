export function formatShortId(id?: string | null) {
  return id ? id.slice(0, 8) : '-';
}

function toNumber(value: string | number | undefined | null): number {
  if (value === undefined || value === null || value === '') {
    return 0;
  }
  return typeof value === 'number' ? value : Number(value);
}

export function formatMoney(value: string | number | undefined | null) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(toNumber(value));
}

export function formatArea(value: string | number | undefined | null) {
  return `${toNumber(value).toFixed(2)} ㎡`;
}

export function formatFriendlyTime(value?: string | null, now = new Date()) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfYesterday = new Date(startOfToday);
  startOfYesterday.setDate(startOfToday.getDate() - 1);
  const pad = (input: number) => String(input).padStart(2, '0');
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;

  if (date >= startOfToday) {
    return `今天 ${time}`;
  }
  if (date >= startOfYesterday && date < startOfToday) {
    return `昨天 ${time}`;
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${time}`;
}

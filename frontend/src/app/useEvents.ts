import { message } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { buildEventSelectOptions, buildValueEnum, type EventLookupSource } from './display';
import { apiGet } from './request';

export function useEvents() {
  const [events, setEvents] = useState<EventLookupSource[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;

    async function loadEvents() {
      setLoading(true);
      try {
        const loadedEvents = await apiGet<EventLookupSource[]>('/api/events');
        if (alive) setEvents(loadedEvents);
      } catch {
        if (alive) {
          setEvents([]);
          message.error('展会列表加载失败，请稍后重试');
        }
      } finally {
        if (alive) setLoading(false);
      }
    }

    loadEvents();
    return () => {
      alive = false;
    };
  }, []);

  const eventOptions = useMemo(() => buildEventSelectOptions(events), [events]);
  const eventValueEnum = useMemo(() => buildValueEnum(eventOptions), [eventOptions]);

  return { events, eventOptions, eventValueEnum, loading };
}

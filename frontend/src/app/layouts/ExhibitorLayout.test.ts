import { describe, expect, it } from 'vitest';
import {
  getExhibitorHeaderConfig,
  getExhibitorNavigationItems,
  getExhibitorSelectedNavKey,
  getExhibitorShellClassNames,
} from './ExhibitorLayout';

describe('exhibitor mobile shell model', () => {
  it('uses a mobile app shell with bottom navigation instead of a desktop header menu', () => {
    expect(getExhibitorShellClassNames()).toEqual({
      root: 'exhibitor-mobile-shell',
      content: 'exhibitor-mobile-content',
      bottomNav: 'exhibitor-bottom-nav',
    });
  });

  it('keeps exhibitor navigation small and action-oriented', () => {
    expect(getExhibitorNavigationItems().map((item) => item.label)).toEqual([
      '首页',
      '报名',
      '订单',
      '通知',
    ]);
  });

  it('selects the most specific exhibitor navigation item', () => {
    expect(getExhibitorSelectedNavKey('/exhibitor/registrations/new')).toBe('/exhibitor/registrations');
    expect(getExhibitorSelectedNavKey('/exhibitor/orders/abc')).toBe('/exhibitor/orders');
    expect(getExhibitorSelectedNavKey('/exhibitor/notifications')).toBe('/exhibitor/notifications');
  });

  it('puts return navigation in the mobile app header for order subpages', () => {
    expect(getExhibitorHeaderConfig('/exhibitor')).toMatchObject({ title: '参展服务' });
    expect(getExhibitorHeaderConfig('/exhibitor')).not.toHaveProperty('backTo');
    expect(getExhibitorHeaderConfig('/exhibitor/registrations')).toMatchObject({ title: '报名进度' });
    expect(getExhibitorHeaderConfig('/exhibitor/registrations/new')).toMatchObject({
      title: '提交报名',
      backTo: '/exhibitor/registrations',
    });
    expect(getExhibitorHeaderConfig('/exhibitor/registrations/r1')).toMatchObject({
      title: '报名详情',
      backTo: '/exhibitor/registrations',
    });
    expect(getExhibitorHeaderConfig('/exhibitor/profile')).toMatchObject({
      title: '企业资料',
      backTo: '/exhibitor',
    });
    expect(getExhibitorHeaderConfig('/exhibitor/orders/new')).toMatchObject({
      title: '我要参展',
      backTo: '/exhibitor',
    });
    expect(getExhibitorHeaderConfig('/exhibitor/orders/5b564321-d0d5-4092-8e6c-eeb7a5ac2b87')).toMatchObject({
      title: '订单详情',
      backTo: '/exhibitor/orders',
    });
    expect(getExhibitorHeaderConfig('/exhibitor/orders/5b564321-d0d5-4092-8e6c-eeb7a5ac2b87/payment')).toMatchObject({
      title: '提交付款',
      backTo: '/exhibitor/orders/5b564321-d0d5-4092-8e6c-eeb7a5ac2b87',
    });
    expect(getExhibitorHeaderConfig('/exhibitor/orders/5b564321-d0d5-4092-8e6c-eeb7a5ac2b87/selection')).toMatchObject({
      title: '选择展位',
      backTo: '/exhibitor/orders/5b564321-d0d5-4092-8e6c-eeb7a5ac2b87',
    });
  });
});

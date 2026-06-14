import { createElement } from 'react';
import { renderToString } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';
import { isMobileWidth, useIsMobile } from './responsive';

function MobileProbe() {
  return createElement('span', null, useIsMobile() ? 'mobile' : 'desktop');
}

describe('responsive foundation', () => {
  it('detects mobile widths below the desktop breakpoint', () => {
    expect(isMobileWidth(375)).toBe(true);
    expect(isMobileWidth(767)).toBe(true);
    expect(isMobileWidth(768)).toBe(false);
  });

  it('renders the mobile hook safely when window is absent', () => {
    vi.stubGlobal('window', undefined);

    try {
      expect(renderToString(createElement(MobileProbe))).toContain('desktop');
    } finally {
      vi.unstubAllGlobals();
    }
  });
});

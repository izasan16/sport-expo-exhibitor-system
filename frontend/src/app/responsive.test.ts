import { describe, expect, it } from 'vitest';
import { isMobileWidth } from './responsive';

describe('responsive foundation', () => {
  it('detects mobile widths below the desktop breakpoint', () => {
    expect(isMobileWidth(375)).toBe(true);
    expect(isMobileWidth(767)).toBe(true);
    expect(isMobileWidth(768)).toBe(false);
  });
});

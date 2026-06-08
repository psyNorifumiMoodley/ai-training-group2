import { inject, Injectable, signal } from '@angular/core';
import { ThemePreference } from '../models/user-settings.model';
import { DOCUMENT } from '@angular/common';

const THEME_KEY = 'dap_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  readonly currentTheme = signal<ThemePreference>(this.loadCachedTheme());

  applyTheme(theme: ThemePreference): void {
    const body = this.document.body;
    if (theme === 'DARK') {
      body.classList.add('dark-theme');
      body.classList.remove('light-theme');
    } else {
      body.classList.add('light-theme');
      body.classList.remove('dark-theme');
    }
    localStorage.setItem(THEME_KEY, theme);
    this.currentTheme.set(theme);
  }

  private loadCachedTheme(): ThemePreference {
    const cached = localStorage.getItem(THEME_KEY);
    if (cached === 'DARK' || cached === 'LIGHT') {
      return cached;
    }
    return 'LIGHT';
  }
}

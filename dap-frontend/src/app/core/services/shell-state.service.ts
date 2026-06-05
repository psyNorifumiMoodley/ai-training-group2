import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ShellStateService {
  readonly sidebarOpen = signal(true);

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }
}

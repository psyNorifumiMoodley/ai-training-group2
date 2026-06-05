import { ChangeDetectionStrategy, Component, HostListener, computed, inject, signal } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ShellStateService } from '../../../core/services/shell-state.service';
import { AvatarComponent } from '../avatar/avatar.component';

@Component({
  selector: 'dap-topbar',
  standalone: true,
  imports: [AvatarComponent, TitleCasePipe],
  templateUrl: './topbar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly shellState = inject(ShellStateService);

  readonly displayName = computed(() => {
    const email = this.authService.currentUser()?.email ?? '';
    return email.split('@')[0].replace(/[._\-]/g, ' ');
  });

  readonly dropdownOpen = signal(false);

  toggleDropdown(): void {
    this.dropdownOpen.update(v => !v);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('[data-user-menu]')) {
      this.dropdownOpen.set(false);
    }
  }

  navigateToSettings(): void {
    this.dropdownOpen.set(false);
    this.router.navigate(['/settings']);
  }

  logout(): void {
    this.dropdownOpen.set(false);
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}

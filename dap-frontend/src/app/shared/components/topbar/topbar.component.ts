import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { AvatarComponent } from '../avatar/avatar.component';

@Component({
  selector: 'dap-topbar',
  standalone: true,
  imports: [AvatarComponent],
  templateUrl: './topbar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  private readonly authService = inject(AuthService);

  readonly displayName = computed(() => {
    const email = this.authService.currentUser()?.email ?? '';
    return email.split('@')[0].replace(/[._\-]/g, ' ');
  });
}

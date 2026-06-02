import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonComponent } from '../button/button.component';

@Component({
  selector: 'dap-confirm-modal',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './confirm-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmModalComponent {
  readonly title   = input('Are you sure?');
  readonly message = input('This action cannot be undone.');
  readonly confirmLabel = input('Delete');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}

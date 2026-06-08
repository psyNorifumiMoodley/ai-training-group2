import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AssessmentStatus } from '../../../core/models/assessment.model';

const STATUS_CLASSES: Record<AssessmentStatus, string> = {
  PENDING:     'bg-orange-100 text-orange-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  SUBMITTED:   'bg-blue-100 text-blue-800',
  MARKED:      'bg-green-100 text-green-800',
};

const STATUS_LABELS: Record<AssessmentStatus, string> = {
  PENDING:     'Pending',
  IN_PROGRESS: 'In progress',
  SUBMITTED:   'Submitted',
  MARKED:      'Marked',
};

@Component({
  selector: 'dap-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span [class]="'inline-block rounded-badge px-[0.4375rem] py-0.5 text-[0.6875rem] font-medium ' + statusClass()">
      {{ label() ?? statusLabel() }}
    </span>
  `,
})
export class BadgeComponent {
  readonly status = input.required<AssessmentStatus>();
  readonly label  = input<string | undefined>(undefined);

  statusClass(): string { return STATUS_CLASSES[this.status()]; }
  statusLabel(): string { return STATUS_LABELS[this.status()]; }
}

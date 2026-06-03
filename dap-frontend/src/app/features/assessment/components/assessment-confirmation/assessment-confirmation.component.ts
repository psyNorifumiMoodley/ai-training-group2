import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'dap-assessment-submitted-confirmation',
  standalone: true,
  templateUrl: './assessment-confirmation.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssessmentConfirmationComponent {}

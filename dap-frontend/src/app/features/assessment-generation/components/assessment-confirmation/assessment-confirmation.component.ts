import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { AssessmentResponse } from '../../../../core/models/assessment.model';

@Component({
  selector: 'dap-assessment-confirmation',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bg-white border border-gray-200 rounded-card p-6 max-w-xl">
      <!-- Success header -->
      <div class="flex items-center gap-3 mb-6">
        <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
          <i class="pi pi-check text-green-600 text-lg"></i>
        </div>
        <div>
          <h2 class="text-sm font-medium text-gray-900">Assessment generated</h2>
          <p class="text-[0.6875rem] text-gray-400">The invitation link is ready to share with the candidate.</p>
        </div>
      </div>

      <!-- Detail rows -->
      <dl class="space-y-3 text-caption mb-6">
        <div class="flex justify-between border-b border-gray-50 pb-3">
          <dt class="text-gray-500">Candidate</dt>
          <dd class="font-medium text-gray-800">{{ candidateName() }}</dd>
        </div>
        <div class="flex justify-between border-b border-gray-50 pb-3">
          <dt class="text-gray-500">Questions</dt>
          <dd class="font-medium text-gray-800">{{ questionCount() }}</dd>
        </div>
        <div class="flex justify-between border-b border-gray-50 pb-3">
          <dt class="text-gray-500">Time limit</dt>
          <dd class="font-medium text-gray-800">{{ assessment().timeLimitMinutes }} min</dd>
        </div>
        <div class="flex justify-between">
          <dt class="text-gray-500">Status</dt>
          <dd class="font-medium text-gray-800">{{ assessment().status }}</dd>
        </div>
      </dl>

      <!-- Invitation link -->
      <div class="bg-gray-50 border border-gray-200 rounded-input p-3">
        <p class="text-[0.6875rem] font-medium text-gray-400 mb-1.5">Invitation link</p>
        <div class="flex items-center gap-2">
          <p class="text-caption text-gray-700 break-all flex-1 select-all">{{ assessment().invitationLink }}</p>
          <button
            (click)="copyLink()"
            [class]="'flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 text-[0.6875rem] font-medium rounded-input border transition-colors ' +
              (copied() ? 'border-green-300 bg-green-50 text-green-600' : 'border-gray-300 text-gray-500 hover:bg-gray-100')">
            <i [class]="'pi text-xs ' + (copied() ? 'pi-check' : 'pi-copy')"></i>
            {{ copied() ? 'Copied!' : 'Copy' }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class AssessmentConfirmationComponent {
  readonly assessment    = input.required<AssessmentResponse>();
  readonly candidateName = input.required<string>();
  readonly questionCount = input.required<number>();

  readonly copied = signal(false);

  copyLink(): void {
    navigator.clipboard.writeText(this.assessment().invitationLink).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }
}

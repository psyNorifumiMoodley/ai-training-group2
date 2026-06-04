import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'dap-toast',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="fixed bottom-5 right-5 z-50 flex flex-col gap-2" aria-live="polite">
      @for (toast of toastService.toasts(); track toast.id) {
        <div [class]="'flex items-center gap-3 px-4 py-3 rounded-card shadow-lg border text-caption bg-white min-w-64 max-w-sm ' +
          (toast.type === 'success' ? 'border-green-200' : 'border-red-200')">
          <i [class]="'pi text-sm flex-shrink-0 ' +
            (toast.type === 'success' ? 'pi-check-circle text-green-500' : 'pi-times-circle text-red-500')"></i>
          <span class="flex-1 text-gray-800">{{ toast.message }}</span>
          <button
            (click)="toastService.dismiss(toast.id)"
            class="flex-shrink-0 text-gray-400 hover:text-gray-600 transition-colors">
            <i class="pi pi-times text-xs"></i>
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastComponent {
  protected readonly toastService = inject(ToastService);
}

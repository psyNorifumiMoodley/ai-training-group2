import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService, ToastType } from '../../../core/services/toast.service';

const BG_CLASS: Record<ToastType, string> = {
  success: 'bg-green-600 border-green-700',
  update:  'bg-blue-600 border-blue-700',
  delete:  'bg-red-600 border-red-700',
  error:   'bg-red-600 border-red-700',
};

const ICON_CLASS: Record<ToastType, string> = {
  success: 'pi-check-circle',
  update:  'pi-pencil',
  delete:  'pi-trash',
  error:   'pi-exclamation-circle',
};

@Component({
  selector: 'dap-toast',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AsyncPipe],
  template: `
    <div class="fixed bottom-6 right-6 z-50 flex flex-col gap-3" aria-live="polite">
      @for (toast of toastService.toasts$ | async; track toast.id) {
        <div [class]="'flex items-center gap-4 px-5 py-4 rounded-card shadow-xl border text-body text-white min-w-80 max-w-md ' + bgClass(toast.type)">
          <i [class]="'pi text-lg flex-shrink-0 ' + iconClass(toast.type)"></i>
          <span class="flex-1 font-medium">{{ toast.message }}</span>
          <button
            (click)="toastService.dismiss(toast.id)"
            class="flex-shrink-0 text-white/70 hover:text-white transition-colors ml-2">
            <i class="pi pi-times text-sm"></i>
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastComponent {
  protected readonly toastService = inject(ToastService);

  bgClass(type: ToastType): string   { return BG_CLASS[type]; }
  iconClass(type: ToastType): string { return ICON_CLASS[type]; }
}

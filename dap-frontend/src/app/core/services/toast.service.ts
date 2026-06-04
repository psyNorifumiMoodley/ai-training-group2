import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'update' | 'delete' | 'error';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  success(message: string): void { this.add('success', message); }
  update(message: string): void  { this.add('update',  message); }
  delete(message: string): void  { this.add('delete',  message); }
  error(message: string): void   { this.add('error',   message); }

  dismiss(id: string): void {
    this._toasts.update(ts => ts.filter(t => t.id !== id));
  }

  private add(type: ToastType, message: string): void {
    const id = crypto.randomUUID();
    this._toasts.update(ts => [...ts, { id, type, message }]);
    setTimeout(() => this.dismiss(id), 4000);
  }
}

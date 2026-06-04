import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastType = 'success' | 'update' | 'delete' | 'error';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts$ = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this._toasts$.asObservable();

  success(message: string): void { this.add('success', message); }
  update(message: string): void  { this.add('update',  message); }
  delete(message: string): void  { this.add('delete',  message); }
  error(message: string): void   { this.add('error',   message); }

  dismiss(id: string): void {
    this._toasts$.next(this._toasts$.getValue().filter(t => t.id !== id));
  }

  private add(type: ToastType, message: string): void {
    const id = crypto.randomUUID();
    this._toasts$.next([...this._toasts$.getValue(), { id, type, message }]);
    setTimeout(() => this.dismiss(id), 4000);
  }
}

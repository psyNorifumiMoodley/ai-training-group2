import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TopbarComponent } from '../../shared/components/topbar/topbar.component';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';
import { ToastComponent } from '../../shared/components/toast/toast.component';

@Component({
  selector: 'dap-shell',
  standalone: true,
  imports: [RouterOutlet, TopbarComponent, SidebarComponent, ToastComponent],
  templateUrl: './shell.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {}

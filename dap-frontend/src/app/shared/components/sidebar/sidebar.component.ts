import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Role } from '../../../core/models/auth.model';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard',      icon: 'pi-home',      route: '/dashboard',      roles: ['ADMIN', 'MARKER'] },
  { label: 'Candidates',     icon: 'pi-users',     route: '/candidates',     roles: ['ADMIN', 'MARKER'] },
  { label: 'Assessments',    icon: 'pi-list',      route: '/assessments',    roles: ['ADMIN', 'MARKER'] },
  { label: 'Question banks', icon: 'pi-folder',    route: '/question-banks', roles: ['ADMIN', 'MARKER'] },
  { label: 'Questions',      icon: 'pi-list-check', route: '/questions',      roles: ['ADMIN', 'MARKER'] },
  { label: 'Markers',        icon: 'pi-id-card',   route: '/markers',        roles: ['ADMIN'] },
  { label: 'Settings',       icon: 'pi-cog',       route: '/settings',       roles: ['ADMIN'] },
];

@Component({
  selector: 'dap-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  readonly visibleItems = computed(() => {
    const role = this.authService.currentUser()?.role;
    return NAV_ITEMS.filter(item => !role || item.roles.includes(role));
  });
}

## 1. Shell Layout Restructure

- [x] 1.1 Update `ShellComponent` template (`features/shell/shell.component.html`) to wrap the layout in a flex-column container with `<dap-topbar />` as the first row, and a flex-row container holding `<dap-sidebar />` and `<main>` as the second row
- [ ] 1.2 Verify the topbar renders edge-to-edge above the sidebar and content area at all viewport widths

## 2. Collapsible Sidebar

- [x] 2.1 Add `collapsed = signal(false)` to `SidebarComponent` (`shared/components/sidebar/sidebar.component.ts`)
- [x] 2.2 Add a toggle button (chevron/hamburger icon) in the sidebar template that calls `collapsed.update(v => !v)`
- [x] 2.3 Apply conditional width classes to the `<nav>` element: `w-[11rem]` when expanded, `w-10` when collapsed; add a CSS transition for smooth resize
- [x] 2.4 Wrap nav item labels in an `@if (!collapsed())` block so only icons are visible in collapsed state
- [ ] 2.5 Verify the main content area (`flex-1`) expands to fill freed space when the sidebar collapses

## 3. Title Case Display Name

- [x] 3.1 Import Angular's built-in `TitleCasePipe` from `@angular/common` in `TopbarComponent`
- [x] 3.2 Apply `| titlecase` pipe to `{{ displayName() }}` in the topbar template wherever the user name is displayed

## 4. User Dropdown Menu

- [x] 4.1 Add `dropdownOpen = signal(false)` to `TopbarComponent` (`shared/components/topbar/topbar.component.ts`)
- [x] 4.2 Replace the standalone Logout button in the topbar template with a click-toggled trigger on the user name/avatar that calls `dropdownOpen.update(v => !v)`
- [x] 4.3 Add a dropdown panel below the trigger (using Tailwind absolute positioning) containing Settings and Logout items, rendered conditionally with `@if (dropdownOpen())`
- [x] 4.4 Wire the Settings item to navigate to `/settings` via `Router.navigate()` and close the dropdown
- [x] 4.5 Wire the Logout item to call `authService.logout()` and close the dropdown
- [x] 4.6 Add a `@HostListener('document:click', ['$event'])` handler in `TopbarComponent` that closes the dropdown when a click occurs outside the user menu element (use a `data-user-menu` attribute to identify the boundary)

## 5. Remove Settings from Sidebar

- [x] 5.1 Remove the Settings entry from the `navItems` array in `SidebarComponent` so it no longer appears as a sidebar navigation item

## 6. Smoke Test

- [ ] 6.1 Start the frontend dev server and verify the topbar spans full width above the sidebar on the dashboard page
- [ ] 6.2 Toggle the sidebar collapse and confirm labels hide, icons remain, and content area reflows
- [ ] 6.3 Confirm the user display name appears in title case in the topbar for a test account
- [ ] 6.4 Click the user name, confirm the dropdown opens with Settings and Logout items, and confirm outside-click closes it
- [ ] 6.5 Click Settings and confirm navigation to the settings route
- [ ] 6.6 Click Logout and confirm session is cleared and redirect to login
- [ ] 6.7 Confirm Settings no longer appears in the sidebar nav list

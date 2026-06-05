### Requirement: Header spans full viewport width above the sidebar and content
The shell layout SHALL render the topbar as a full-width row above both the side navigation and the main content area, so that the header is never visually alongside or behind the sidebar.

#### Scenario: Header is visually above the sidebar on all authenticated pages
- **WHEN** an authenticated user navigates to any protected route
- **THEN** the topbar element occupies the top of the viewport and its bottom edge aligns flush above the top edge of both the sidebar and the main content area

#### Scenario: Header width matches the full viewport
- **WHEN** the page is rendered at any supported viewport width
- **THEN** the topbar element stretches edge-to-edge with no horizontal gap

---

### Requirement: Sidebar is collapsible to an icon-only rail
The side navigation SHALL support a collapsed state that reduces it to a narrow icon-only rail, toggled by a visible control available without leaving the current page.

#### Scenario: Sidebar collapses on toggle
- **WHEN** the user activates the collapse toggle
- **THEN** the sidebar width narrows to an icon-only rail, nav item labels are hidden, and only the navigation icons remain visible

#### Scenario: Sidebar expands on toggle
- **WHEN** the sidebar is collapsed and the user activates the expand toggle
- **THEN** the sidebar returns to its full width and nav item labels are visible again

#### Scenario: Main content area reflows on collapse
- **WHEN** the sidebar is collapsed
- **THEN** the main content area expands to fill the freed horizontal space

#### Scenario: Collapsed state is not persisted across page reloads
- **WHEN** the user reloads the page
- **THEN** the sidebar is shown in its default expanded state

---

### Requirement: Authenticated user's display name is formatted in title case
The shell topbar SHALL display the authenticated user's name in title case (first letter of each word capitalised, remaining letters lowercase) wherever the name appears in the shell header.

#### Scenario: Display name rendered in title case
- **WHEN** an authenticated user is logged in with a display name in any casing (e.g., "JOHN DOE", "john doe", "john DOE")
- **THEN** the name is rendered as "John Doe" in the topbar

---

### Requirement: User dropdown menu accessible from the topbar
The topbar SHALL provide a user dropdown menu, triggered by clicking the user's name or avatar, containing at minimum a Settings item and a Logout item.

#### Scenario: Dropdown opens on user name click
- **WHEN** the user clicks on their name or avatar in the topbar
- **THEN** a dropdown menu appears below the trigger with at least Settings and Logout items visible

#### Scenario: Dropdown closes on outside click
- **WHEN** the dropdown is open and the user clicks anywhere outside the dropdown panel
- **THEN** the dropdown closes without triggering navigation or logout

#### Scenario: Settings item navigates to settings page
- **WHEN** the dropdown is open and the user clicks Settings
- **THEN** the dropdown closes and the application navigates to the settings route

#### Scenario: Logout item ends the session
- **WHEN** the dropdown is open and the user clicks Logout
- **THEN** the user session is cleared and the application redirects to the login page

#### Scenario: Settings is not a standalone sidebar nav item
- **WHEN** an authenticated user views the sidebar
- **THEN** Settings does not appear as a nav item in the sidebar navigation list

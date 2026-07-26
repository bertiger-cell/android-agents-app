# DESIGN.md – Android Agents App

## Design Philosophy

Electric dark mode with high-contrast accents. Tech/cyber aesthetic
with deep backgrounds and vibrant violet-cyan highlights. Clean,
functional, no visual clutter. Every element must justify its presence.

## Color System

### Dark Mode (Primary)
- Background: #0D0D0D (near-black, not pure black)
- Surface: #1A1A1A (cards, inputs)
- Surface Variant: #252525 (elevated elements)
- Primary: #BB86FC (vibrant violet — CTAs, active states)
- Primary Container: #3700B3 (deep violet — headers, emphasis)
- Secondary: #03DAC6 (cyan — tags, chips, secondary actions)
- Secondary Container: #018786 (teal — badges)
- Tertiary: #FF6B6B (coral red — errors, warnings, highlights)
- Outline: #938F99 (subtle borders, dividers)
- On-Background: #E6E1E5 (primary text — near-white, not pure white)
- On-Primary: #000000 (text on violet buttons)
- On-Surface: #E6E1E5 (text on dark surfaces)
- On-Surface Variant: #CAC4D0 (secondary text, labels)

### Contrast Rules
- Never use pure white (#FFFFFF) on pure black (#000000)
- Minimum 4.5:1 contrast ratio for body text
- Primary action buttons: violet (#BB86FC) with black text
- Danger actions: coral (#FF6B6B) with white text
- Disabled states: 38% opacity on primary

## Typography

### Font Stack
- Primary: System default (Roboto on Android)
- Monospace: For code/technical content

### Type Scale
- Display Large: 57sp / 64sp line-height (splash clock)
- Headline Medium: 28sp / 36sp (screen titles)
- Title Medium: 16sp / 24sp / Medium weight (section headers)
- Body Large: 16sp / 24sp (primary content)
- Body Small: 12sp / 16sp (labels, metadata)
- Label Large: 14sp / 20sp / Medium weight (button text)

## Spacing & Layout

### Grid
- Base unit: 4dp
- Screen padding: 16dp horizontal
- Section spacing: 24dp
- Element spacing: 8dp-16dp
- Card padding: 16dp

### Elevation & Depth
- Cards: surfaceTonal with 2dp tonal elevation
- Buttons: No shadow, color contrast for depth
- FAB: Primary container color, 16dp padding
- TopAppBar: Primary color, no elevation

## Component Patterns

### Buttons
- Primary (Filled): Rounded 12dp, primary color (#BB86FC), black text
- Secondary (Outlined): Rounded 12dp, outline border (#938F99), on-surface text
- Danger (Filled): Rounded 12dp, tertiary color (#FF6B6B), white text
- Disabled: 38% opacity, no interaction

### Cards
- Rounded 16dp corners
- Surface color (#1A1A1A) with tonal elevation
- Subtle outline border (1dp, #938F99 at 30% opacity)
- Hover/press: surfaceTonal shift

### Input Fields (OutlinedTextField)
- Rounded 12dp corners
- Outline border (#938F99)
- Focus: Primary color border (#BB86FC)
- Background: Transparent (surface shows through)

### TopAppBar
- Primary container color (#3700B3)
- White icons and text
- No elevation (flat)

### Chat Bubbles
- User: Primary color (#BB86FC), black text, right-aligned
- Assistant: Surface (#1A1A1A), on-surface text, left-aligned
- Max width: 300dp
- Rounded 16dp corners
- 12dp internal padding

### Navigation
- Bottom: Not used (top navigation only)
- Top: Back arrow + title in TopAppBar
- Transitions: Fade + slide (400ms)

## Animation & Motion

### Timing
- Screen transitions: 400ms fade + slide
- Element appearance: 300ms fade-in
- Button press: 100ms scale-down (0.95)
- List stagger: 50ms delay between items

### Easing
- Standard: FastOutSlowInEasing
- Sharp: LinearEasing (for continuous animations like intro walk)

## Iconography
- Material Icons (Filled style)
- Size: 24dp default
- Color: Inherits from parent (on-primary, on-surface)

## Special Elements

### Intro Screen
- Robot emoji (🤖) walking across screen
- Thought bubble with rounded 20dp corners
- Bubble tail: 12dp square with 4dp rounding
- Background: App background color
- Duration: 4.2 seconds total

### Agent Cards
- Left: Name (titleMedium) + description (bodySmall)
- Right: Play + Delete icons
- Bottom: Agent type + provider chips (AssistChip)

### Model Picker
- ExposedDropdownMenu with 12dp rounding
- Model name (bodyLarge) + details (bodySmall)
- Max 3 items visible before scroll

## Screen Layouts

### HomeScreen
- Clock: Display Large, primary color, left-aligned
- Date: Title Medium, on-surface-variant, below clock
- Divider: 1dp outline
- Recent agents: Card list, max 3 items
- Bottom: Full-width primary button

### ChatScreen
- TopBar: Agent name (titleMedium) + model (bodySmall)
- Messages: LazyColumn, 8dp spacing
- Input: Row with OutlinedTextField + Send IconButton
- Auto-scroll on new tokens

### SettingsScreen
- Sections: Provider name (titleMedium) + divider
- Inputs: OutlinedTextField with show/hide toggle
- Test button: OutlinedButton
- Save: Full-width primary Button

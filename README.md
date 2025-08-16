# Shopping List Android Application (רשימת קניות)

A comprehensive Android shopping list application with Hebrew UI and input support, featuring two distinct modes: Planning and Shopping, designed to help users organize their grocery shopping efficiently.

## Features

### Planning Mode
- **Create and Edit Lists**: Add, edit, and remove items from your shopping list
- **Section Organization**: Organize items by grocery store sections (Produce, Dairy, Meat, etc.)
- **Section Management**: 
  - Add new sections
  - Edit section names
  - Delete sections (items move to "Unassigned" section)
- **Item Management**:
  - Add items to specific sections
  - Edit item names
  - Delete items
  - Move items between sections
- **Visual Organization**: Collapsible sections for better list management

### Shopping Mode
- **Check-off Items**: Mark items as purchased with checkboxes
- **Visual Feedback**: Checked items are grayed out and struck through
- **Uncheck Capability**: Accidentally checked items can be unchecked
- **Ad-hoc Items**: Add new items on the fly while shopping
- **Read-only Sections**: Cannot edit or delete sections while shopping
- **Progress Tracking**: See counts of remaining and completed items

### UI/UX Features
- **Hebrew Language Support**: Complete Hebrew UI with RTL (Right-to-Left) text direction
- **Hebrew Input**: Native Hebrew text input support
- **Warm and Welcoming Design**: Beautiful color scheme with purple and pink accents
- **Phone-Optimized**: Designed specifically for mobile form factor
- **Intuitive Interface**: Easy-to-use buttons and gestures
- **Material Design**: Follows Android Material Design guidelines
- **Responsive Layout**: Adapts to different screen sizes

## Technical Architecture

### Data Layer
- **Room Database**: Local SQLite database with Room ORM
- **Entities**: 
  - `ShoppingItem`: Individual shopping items
  - `Section`: Grocery store sections
  - `SectionWithItems`: Relationship between sections and items
- **DAO**: Data Access Object for database operations
- **Repository**: Single source of truth for data operations

### UI Layer
- **MVVM Architecture**: Model-View-ViewModel pattern
- **LiveData**: Reactive data binding
- **RecyclerView**: Efficient list rendering with custom adapters
- **ViewBinding**: Type-safe view access

### Key Components
- **MainActivity**: Main UI controller
- **ShoppingViewModel**: Business logic and state management
- **SectionAdapter**: Manages section display and interactions
- **ItemAdapter**: Manages individual item display and interactions

## Database Schema

### Sections Table
- `id`: Primary key
- `name`: Section name
- `orderIndex`: Display order
- `isDefault`: Whether this is the default "Unassigned" section

### Shopping Items Table
- `id`: Primary key
- `name`: Item name
- `sectionId`: Foreign key to sections table
- `isChecked`: Whether item is purchased
- `isAdHoc`: Whether item was added during shopping
- `orderIndex`: Display order within section

## Default Data

The application comes pre-populated with Hebrew content:
- **Default Section**: "לא מוקצה" (for items without a section)
- **Sample Sections**: ירקות ופירות, מוצרי חלב, בשר, מזווה, קפוא, משקאות, בית
- **Sample Items**: Common Hebrew grocery items organized by section

## Usage Instructions

### Planning Your Shopping List
1. **Add Items**: Tap the "+" button in any section to add items
2. **Create Sections**: Use the floating action button to add new sections
3. **Organize Items**: Use the move button to reorganize items between sections
4. **Edit Names**: Tap edit buttons to modify item or section names
5. **Delete**: Remove unwanted items or sections

### Shopping at the Store
1. **Switch to Shopping Mode**: Tap the "תכנון" button to switch to "קניות"
2. **Check Off Items**: Tap checkboxes to mark items as purchased
3. **Add Ad-hoc Items**: Use the "+" button to add items you forgot
4. **Track Progress**: Monitor the item counts at the top
5. **Uncheck Mistakes**: Tap checkboxes again to uncheck items

## Color Scheme

- **Primary**: Purple (#FF6B73FF)
- **Accent**: Pink (#FFFF6B9D)
- **Background**: Light gray (#FFF8F9FA)
- **Surface**: White (#FFFFFFFF)
- **Text**: Dark gray (#FF202124)
- **Success**: Green (#FF34A853)
- **Error**: Red (#FFEA4335)

## Dependencies

- **Room**: Database ORM
- **LiveData**: Reactive programming
- **ViewModel**: UI state management
- **RecyclerView**: List display
- **Material Design**: UI components
- **Coroutines**: Asynchronous programming

## Building and Running

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on an Android device or emulator

## Requirements

- Android API Level 24+ (Android 7.0+)
- Android Studio Arctic Fox or later
- Kotlin 1.8+

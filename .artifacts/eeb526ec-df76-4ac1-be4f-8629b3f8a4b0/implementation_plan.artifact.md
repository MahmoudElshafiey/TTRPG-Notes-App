# Fix Performance and Background Image Feature

This plan addresses the UI stutters during image viewing/drawing and implements a proper background image feature for notes, resolving the conflict where drawing paths and images were using the same field.

## User Review Required

> [!IMPORTANT]
> **Database Changes**: I am adding a new field `backgroundImage` to the `Note` entity. A destructive migration or a specific migration is required. I will attempt a non-destructive migration first.
> [!WARNING]
> **Memory Usage**: Storing large Base64 strings in the database is a primary cause of slowness. I will implement asynchronous decoding and scaling to mitigate UI lag, but for long-term health, moving to file-based storage is recommended (not included in this immediate fix to avoid breaking data).

## Proposed Changes

### Data Layer

#### [MODIFY] [Note.kt](file:///D:/TTRPG_Notes/app/src/main/java/com/example/dndnotes/data/model/Note.kt)
- Add `backgroundImage: String? = null` field to store the Base64 background image separate from the drawing paths.

#### [MODIFY] [AppDatabase.kt](file:///D:/TTRPG_Notes/app/src/main/java/com/example/dndnotes/data/local/AppDatabase.kt)
- Increment database version to 5.
- Add migration logic for the new `backgroundImage` column.

---

### UI & Performance

#### [MODIFY] [NoteEditorScreen.kt](file:///D:/TTRPG_Notes/app/src/main/java/com/example/dndnotes/ui/screens/NoteEditorScreen.kt)
- **Async Decoding**: Move `Base64.decode` and `BitmapFactory.decodeByteArray` out of the composition's main path in `ImageItem`.
- **Set Background**: Add an action in `ImagesTab` to set an attached image as the note's background.

#### [MODIFY] [DrawingCanvas.kt](file:///D:/TTRPG_Notes/app/src/main/java/com/example/dndnotes/ui/components/DrawingCanvas.kt)
- Add support for rendering a background image.
- Optimize path rendering to avoid redundant work during drags.

#### [MODIFY] [DrawingTab.kt](file:///D:/TTRPG_Notes/app/src/main/java/com/example/dndnotes/ui/screens/DrawingTab.kt)
- Pass the note's `backgroundImage` to the `DrawingCanvas`.

---

### Business Logic

#### [MODIFY] [NoteViewModel.kt](file:///D:/TTRPG_Notes/app/src/main/java/com/example/dndnotes/ui/screens/NoteViewModel.kt)
- Add `setBackgroundImage` function to update the note's background.

## Verification Plan

### Automated Tests
- Run `ExampleInstrumentedTest.kt` to ensure basic app flow still works.

### Manual Verification
1.  **Image Loading**: Open a note with multiple large images and verify the list scrolls smoothly without UI freezing.
2.  **Background Image**:
    - Upload an image in the "Images" tab.
    - Click "Set as Background".
    - Go to "Drawing" tab and verify the image appears behind the canvas.
    - Draw something over the image, save, and reload to ensure both drawing and background persist.
3.  **Drawing Performance**: Verify that drawing complex shapes doesn't lag the UI.

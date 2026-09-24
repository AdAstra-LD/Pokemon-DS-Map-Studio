If this fork helped you with your project, please remember to credit Trifindo and myself.
This release integrates and builds upon [Trinsic64's fork](https://github.com/Trinsic64/Pokemon-DS-Map-Studio), huge thanks to them.

## Important thing to remember
Maps resaved with this fork will be _**incompatible**_ with vanilla PDSMS and older PDSMS versions. This is still due to the additional 3D layer and Export Group metadata. Tile folder organization is saved in a separate `.meta` sidecar file next to your tileset.

### Selection tools (merged from Trinsic64's fork, refined)
- Rectangle, Lasso and Magic Wand selection, plus a Move Selection tool for dragging selected tiles to a new place.
- Cut, Copy, Paste and Delete for selected regions, with full undo support and selections that work across map boundaries. The clipboard survives opening another map, so tiles can be copied between maps.
- Flip, Rotate and Fill inside selections; Line, Rectangle and Ellipse drawing tools — all with optional Smart Drawing via the "Smart Tools" toggle. Right mouse button draws the inverted form of a Smart shape.
- Shift+drag starts a rectangle selection from any tool: the tile preview disappears and the cursor switches, so you always know you are selecting instead of drawing. Shift+click with the Wand selects all matching tiles in the map; Ctrl+click adds to / removes from the selection.
- Camera controls moved to Ctrl: Ctrl+drag pans, Ctrl+mouse wheel zooms.
- Middle click flood fill now only fills inside the current selection; clicking an unselected tile does nothing.
- Animated selection outline (marching ants); the selection fill turns more transparent while moving tiles.
- Opening or creating a map releases every selection.

| Action | Shortcut |
| --- | --- |
| Select all | `Ctrl+A` |
| Copy / Cut / Paste | `Ctrl+C` / `Ctrl+X` / `Ctrl+V` |
| Fill selection | `Ctrl+F` |
| Deselect | `Ctrl+D` |
| Delete selection | `Delete` |
| Rectangle selection (any tool) | `Shift`+drag |
| Pan / zoom camera | `Ctrl`+drag / `Ctrl`+wheel |
| Flood fill inside selection | Middle click |

### Tile organization (merged from Trinsic64's fork, refined)
- Tile folders for the tile list, including nested subfolders, custom grid layouts with empty slots, folder pinning, resizing and reordering. Folders never change the game-facing tile order.
- Folder tiles can have independent display sizes without changing their real game dimensions.
- Portable folder bundles: export a folder and import it into another compatible tileset, preserving hierarchy, layout slots, tile names, display sizes, collision defaults and Smart Drawing organization — the bundle can even carry tiles not present in the destination tileset.
- Ctrl+click multi-selection, Shift+click range selection and Shift+drag band selection in the tile list. Dragging any selected tile moves the whole group, preserving tileset order.
- Create Smart Drawings from tiles selected in a folder; new Smart Drawing folder system with expandable groups.
- New right-click menus for folder management (create, import, export, rename, display size).

### Collision defaults (merged from Trinsic64's fork, refined)
- Per-tile collision defaults with proper "Type" and "Collision" layer names, editable in the Tileset Editor's new Collision Defaults tab.
- Collision footprints can be larger than the tile's game dimensions, with a configurable anchor.
- Paint or clear cells one at a time, fill or clear a whole layer, right-click to sample a cell's value, and copy/paste a single layer or the complete defaults between tiles.
- "Auto Coll." toggle applies these defaults automatically while mapping.
- Overhauled move permission labels for all supported games: corrected DP/Pt and HGSS terrain types, labeled HGSS footstep sounds, and renamed the BW/B2W2 collision layers to match the actual data layout.

### Replace & Remap
- New safe Replace/Remap workflow: validation and a full plan preview before anything is written to your maps.

### Export improvements
- Visual area picker for NSBTX export and PDSMAP splitting: the map matrix is rendered with area color overlays; Ctrl+wheel zooms at the cursor, middle-drag pans, clicking toggles areas.
- Progress dialog for the PDSMAP area split, with per-area status, counts and error details.
- Fixed the PDSMAP area split: it no longer redirects the open project's saves into the last area folder, each area opens on its own with its collision/BDHC/building files (no extra map at 0,0 anymore), the area thumbnails and "Open Recent" entries are correct, and an unexpected error can no longer lock the progress dialog.
- Reworked and unified export dialogs (NSBMD, NSBTX, IMD, split areas).
- `g3dcvtr.exe` is now located automatically (working folder, app folder, or any parent folder), so exporting works no matter where you launch from. As always, `g3dcvtr.exe` is user-supplied and never redistributed.

### UI and platform
- Native OS file dialogs everywhere (thanks to AsteroidPizza39), including fixes for many save dialogs that previously opened as "Open" dialogs.
- Updated JOGL to 2.5.0 — the app now runs on Apple Silicon Macs (thanks to rene589).
- Corrected the backsound names (thanks to Pixelstyx).
- Fixed a crash when importing OBJ tiles exported from Blockbench (thanks to PoeticRainbow).
- Fixed the Linux install script (thanks to LowriJenkins).
- Compact toolbar layout: two-column tool groups, View group grid with separators, layer up/down buttons under the layer selector, Deselect next to the selection tools.
- The Keyboard Shortcuts window lists all the new bindings.

### Before you use
- Compatible with maps made using PDSMS v2.2.2. **Backup your maps** before opening them in this version.
- Do not import someone else's tileset metadata (`.meta`) over your own maps — it rewrites your tile organization. Build your own folder structure, or start from Trinsic64's starter tileset + metadata for blank maps and testing.

REQUIREMENTS: Java 11 or newer

*Pokemon is a trademark of Nintendo, Creatures Inc., and GAME FREAK inc. This is an unofficial community project, not affiliated with or endorsed by those companies.*

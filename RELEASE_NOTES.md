# Pokemon DS Map Studio 2.3.1 - Enhanced Tools Release

## Download and Packaging Improvements

- Added a ready-to-run Windows package with its own Java runtime and `.exe` launcher.
- Added a portable ZIP with Windows and Linux/macOS launch scripts.
- Included the expected root-level `converter` folder, supporting DLL, and setup instructions.
- Included README and release notes inside each distribution.
- Added automated tagged releases and SHA-256 checksum generation.
- `g3dcvtr.exe` remains user-supplied and is not redistributed.

This release expands Pokemon DS Map Studio with new map-selection tools, Smart Drawing workflows, tile organization, portable metadata, and collision-default editing.

## New Map Selection Tools

- Rectangle selection
- Lasso selection
- Wand selection
- Move selected tiles
- Copy, cut, paste, duplicate, and delete selections
- Rotate and flip selections
- Fill a selection with the currently selected tile
- Selection operations across map boundaries
- Undo support for selection edits

### Selection Shortcuts

| Action | Shortcut |
| --- | --- |
| Select all | `Ctrl+A` |
| Copy | `Ctrl+C` |
| Cut | `Ctrl+X` |
| Paste | `Ctrl+V` |
| Fill selection | `Ctrl+F` |
| Deselect | `Ctrl+D` |
| Delete selection | `Delete` |

Choose a selection tool from the main toolbar, select the required tiles, and then use the toolbar actions, keyboard shortcuts, or selection context menu.

## Smart Tools

- Smart freehand drawing with automatic edges and corners
- Smart line drawing
- Smart rectangle drawing
- Smart circle drawing
- Smart wand selection
- Smart rotation and flipping
- Inverted Smart Drawing shapes using the right mouse button
- Smart Drawing templates generated from arranged folder tiles

Select a Smart Drawing reference, enable **Smart Tools**, and use the normal drawing or shape tools on the map. Draw with the left mouse button for the standard form or the right mouse button for its inverted form.

## Tile Folders

- Create folders and nested subfolders
- Arrange tiles in custom grid layouts
- Keep empty layout spaces without creating placeholder tile IDs
- Drag and drop tiles between folders
- Move or remove tiles using the tile context menu
- Pin folders for access while browsing long tilesets
- Expand, collapse, resize, reorder, and scroll folders
- Give folder tiles independent display sizes without changing game tile dimensions
- Duplicate tile groups while preserving their folder arrangement
- Organize Smart Drawing templates into expandable groups
- Keep folder order independent from the game-facing **All Tiles** order

Right-click a folder or tile in the Tile List to access its organization, layout, display-size, movement, import, and export commands. Folder layouts are available in both the main window and the Tileset Editor.

## Portable Tile Metadata

- Export individual folders as portable bundles
- Import folders without replacing existing folder structures
- Import bundles containing tiles that are not already in the destination tileset
- Preserve folder hierarchy and layout slots
- Preserve tile names and folder display sizes
- Preserve collision defaults
- Preserve Smart Drawing organization

Use the folder context menu to export a folder bundle. Import the bundle into another compatible tileset to add its folder structure and included metadata.

## Collision Defaults

- Define collision defaults for both collision layers
- Create collision footprints larger than a tile's game dimensions
- Use folder display size as the collision-footprint reference
- Paint or clear collision values one cell at a time
- Fill or clear an entire collision layer
- Copy and paste one collision layer
- Copy and paste complete collision defaults
- Edit collision defaults from tile context menus
- Dedicated Collision Defaults workspace in the Tileset Editor
- Automatically apply defaults while drawing with **Auto Coll.**

Open the **Collision Defaults** tab in the Tileset Editor and select a tile from the Tile Selector. Choose a collision value, then left-click or drag to paint cells. Right-click a footprint cell to sample its collision value. Use **Copy Layer**, **Paste Layer**, **Copy All**, and **Paste All** to reuse settings efficiently.

Enable **Auto Coll.** in the main map tools when collision defaults should be applied automatically as tiles are painted.

## Tileset Editor Enhancements

- Multi-tile selection follows the visible folder layout order
- Move and organize folder tiles without changing **All Tiles** ordering
- Duplicate selected tile arrangements into new layout space
- Create Smart Drawing templates from selected folder layouts
- Assign and remove multiple selected tiles from folders
- Edit collision defaults while browsing the tileset

Use the Tile Selector to choose one or more tiles. Folder-based selection and movement operate on the folder arrangement, while **All Tiles** continues to represent the tileset's game-data order.

## Credits

- **Pokemon DS Map Studio:** [AdAstra-LD](https://github.com/AdAstra-LD/Pokemon-DS-Map-Studio) and the original PDSMS contributors
- **Enhanced tools and feature design:** [Trinsic64](https://github.com/Trinsic64)
- **Development assistance:** Claude Code and OpenAI Codex

Pokemon is a trademark of Nintendo, Creatures Inc., and GAME FREAK inc. This is an unofficial community project and is not affiliated with or endorsed by those companies.

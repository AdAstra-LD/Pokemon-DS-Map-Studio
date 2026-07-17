# v2.4 Format and DSPRE Research

Research baseline: PDSMS `2af5715294c2e2e40d20a5160a40d8a38ed9b755` and DSPRE
`962d5d3c02eabc50e51d2772cc312d7bb71a70f1` (2026-07-11).

## Architecture

- `MapMatrix` owns loaded `MapData` objects and PDSMAP serialization. Each `MapData`
  owns a nine-layer `MapGrid`, PER data, BDHC, BDHCAM, BLD placements, and BGS.
- `Tileset` owns game-facing tile IDs. Materials and tile-folder metadata are separate
  concerns and must not be treated as tile IDs during replacement.
- Map undo uses snapshots held by `StateHandler`. v2.4 map transactions expose their
  affected maps/layers through `MapState`; Replace and Remap adds one coherent state.
- The DPPt and HGSS Building Editors already load model, material-order, animation,
  AreaData, building-texture, and area-building archives. HGSS keeps field and room
  model/animation lists separate.
- Building models are parsed and displayed directly as NSBMD through the bundled Nitro
  libraries and `renderer.NitroDisplayGL`. A Building Layer should prototype reuse of
  that geometry/render path before considering cached conversion.

## Capability Matrix

| Resource | Diamond/Pearl | Platinum | HGSS | Black/White and B2W2 |
|---|---|---|---|---|
| PER | 32x32 interleaved type + permission bytes; PDSMS load/save supported | Same Gen IV layout; PDSMS load/save supported | Same Gen IV layout; PDSMS load/save supported | Geometry plus two 16-bit values per cell and optional corner records; PDSMS has a dedicated 3D parser, but generic remapping is unsafe and disabled |
| BDHC | Dedicated DP loader/writer | PDSMS currently routes through its HGSS-family loader/writer; fixture verification required | Dedicated HGSS-family loader/writer | Not the Gen V terrain representation used by PDSMS |
| BDHCAM | Not exposed for D/P | Supported as camera data appended to aligned BDHC | Supported as camera data appended to aligned BDHC | No supported PDSMS/DSPRE adapter |
| BLD | 48-byte Gen IV placements | 48-byte Gen IV placements | 48-byte Gen IV placements | Building Editor and game-specific source loading unavailable |
| Map/model BIN | PDSMS packer exists from PER/BLD/NSBMD/BDHC sidecars | Packer also accepts merged BDHCAM | Packer includes BGS before PER | No PDSMS packer and DSPRE does not support Gen V |
| Building model archives | DPPt Building Editor supports exterior model/texture/animation lists | DPPt family support | Separate field/room models and animation lists | Unsupported by the current Building Editor |

Important BLD limitation: `BuildFile` currently reads model ID, fixed-point position,
and scale, but discards X/Y/Z rotations and unknown bytes. Its writer recreates those
discarded fields as zero. It must be replaced or extended with a raw-preserving record
before BLD synchronization or transform-accurate Building Layer work.

The DP/Pt/HGSS BIN packer classes exist, but the multi-map `saveBinaryMaps` path is
commented out. Treat BIN export as unverified until synthetic round trips and a DSPRE
import/reopen workflow pass.

## Current DSPRE Integration Findings

- The maintained source is https://github.com/DS-Pokemon-Rom-Editor/DSPRE. It currently
  documents Generation IV support, not Generation V.
- DSPRE is a .NET Framework 4.8 WinForms `WinExe`. `Program.Main()` takes no arguments,
  and the current tree exposes no supported CLI or batch map operation.
- The solution contains DSPRE, Ekona, and Images projects, but its map/file classes are
  application code rather than a documented, versioned reusable API.
- The repository is AGPL-3.0. PDSMS should independently implement documented binary
  operations and must not copy DSPRE source or embed its assemblies without a separate
  license decision.
- A project folder ends in `_DSPRE_contents`. DSPRE maps packed ROM paths to stable
  working directories under `unpacked/<DirNames>`, including `maps`, `matrices`,
  `areaData`, `buildingConfigFiles`, `buildingTextures`, `exteriorBuildingModels`, and
  HGSS `interiorBuildingModels`. NARC entries are numeric files, normally four-digit,
  without filename extensions.
- DSPRE caches the selected `MapFile` and other editor objects in memory. External writes
  can leave an open DSPRE window stale and a later DSPRE save can overwrite them. Any
  synchronization apply must warn the user to close or reopen/reload the DSPRE project.

Direct, hash-checked operations on DSPRE's unpacked files are preferable to UI
automation. A future link sidecar should store the DSPRE root, game family, selected
matrix/map mapping, and three-way baseline hashes. Preview must classify each file as
PDSMS to DSPRE, DSPRE to PDSMS, unchanged, or conflicted; conflicts require explicit
resolution. Apply needs backups, temporary writes, atomic replacement, and reopen
validation.

## Fixtures Still Required

- Synthetic 2048-byte Gen IV PER fixtures with known type/permission orientation.
- Synthetic 48-byte BLD records with nonzero rotations, scale, fractional coordinates,
  and unknown bytes to prove preservation.
- Writer-generated minimal DP and HGSS-family BDHC plus BDHCAM fixtures. Platinum must
  be independently reopened in DSPRE before its current routing is accepted.
- Synthetic Gen IV matrix and map BIN bundles for D/P, Pt, and HGSS.
- User-owned, non-committed representative files for all supported regional/game
  variants and for Gen V PER corner records.
- A direct-NSBMD Building Layer prototype that proves shared-context rendering,
  coordinate scale/origin, transforms, missing-model placeholders, and disposal.

No ROM, extracted commercial model, proprietary converter, or user project fixture
belongs in the repository.

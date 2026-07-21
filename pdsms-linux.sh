#!/usr/bin/env bash

VERSION="2.2.2"
APP_NAME="Pokemon DS Map Studio"
APP_DIR="$APP_NAME-$VERSION"
ZIP_NAME="Pokemon.DS.Map.Studio-$VERSION.zip"

# check that unzip is present
command -v unzip >/dev/null 2>&1 || {
    echo >&2 "This script requires unzip!"
    exit 1
}

# check if wget is present
command -v wget >/dev/null 2>&1 || {
    echo >&2 "This script requires wget!"
    exit 1
}

# check if java is present
command -v java >/dev/null 2>&1 || {
    echo >&2 "This script requires java!"
    exit 1
}

# download PDSMS to local user applications
cd ~/.local/share/applications/ || exit
wget "https://github.com/AdAstra-LD/Pokemon-DS-Map-Studio/releases/download/v$VERSION/$ZIP_NAME" || {
    echo >&2 "Download failed!"
    exit 1
}
# the release zip has no top-level folder, so extract into a versioned directory
unzip -o "$ZIP_NAME" -d "$APP_DIR"
# remove the downloaded archive
rm -f "$ZIP_NAME"

# download icon
wget -O "$APP_DIR/icon.png" "https://github.com/AdAstra-LD/Pokemon-DS-Map-Studio/raw/master/src/main/resources/icons/programIconHD.png"

# create a desktop shortcut
echo "#!/usr/bin/env xdg-open
[Desktop Entry]
Type=Application
Name=Pokemon DS Map Studio
Exec=/usr/bin/java -jar \"${PWD}/$APP_DIR/lib/$APP_NAME-$VERSION.jar\"
Icon=${PWD}/$APP_DIR/icon.png
Categories=Development;
" > PDSMS.desktop

echo "Pokemon DS Map Studio has been installed! Enjoy ;)"

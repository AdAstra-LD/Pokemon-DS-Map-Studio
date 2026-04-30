package editor.game.patches;

import editor.game.GameFolder;
import formats.narc2.Narc;
import formats.narc2.NarcFile;
import formats.narc2.NarcIO;
import utils.BinaryReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class MultiFilePatch {

    private String gameCode;
    private List<FilePatch> patches;

    public MultiFilePatch(String gameCode, List<FilePatch> patches) {
        this.gameCode = gameCode;
        this.patches = patches;
    }

    public boolean canApplyPatch(String gameFolderPath) throws Exception{
        GameFolder gameFolder = new GameFolder(gameFolderPath);
        if (!gameFolder.hasGameCode(gameCode)) {
            return false;
        }

        for(FilePatch patch : patches){
            byte[] gameData = loadGameData(gameFolder, patch.getFilePath());
            //byte[] gameData = Files.readAllBytes(new File(patch.getFilePath()).toPath());
            if(!patch.canApplyPatch(gameData)){
                return false;
            }
        }
        return true;
    }

    public boolean isPatched(String gameFolderPath) throws Exception{
        GameFolder gameFolder = new GameFolder(gameFolderPath);
        for(FilePatch patch : patches){
            //byte[] gameData = Files.readAllBytes(new File(patch.getFilePath()).toPath());
            byte[] gameData = loadGameData(gameFolder, patch.getFilePath());
            if(!patch.isPatched(gameData)){
                return false;
            }
        }
        return true;
    }

    public void applyPatch(String gameFolderPath) throws Exception{
        GameFolder gameFolder = new GameFolder(gameFolderPath);
        for(FilePatch patch : patches){
            //byte[] gameData = Files.readAllBytes(new File(patch.getFilePath()).toPath());
            byte[] gameData = loadGameData(gameFolder, patch.getFilePath());
            System.arraycopy(patch.getNewData(), 0, gameData, patch.getDataOffset(), patch.getNewData().length);
        }
    }

    private static byte[] loadGameData(GameFolder gameFolder, String relativePath) throws IOException {
        File file = gameFolder.getFile(relativePath);
        if(file.exists()){
            return Files.readAllBytes(file.toPath());
        }else{
            String[] splitPath = gameFolder.resolveRelativePath(relativePath).replace('\\', '/').split("/");
            File fullPath = new File(gameFolder.getRootFolderPath());

            for(int i = 0; i < splitPath.length; i++){
                fullPath = new File(fullPath, splitPath[i]);
                if(fullPath.isFile()){
                    try {
                        Narc narc = NarcIO.loadNarc(fullPath.getPath());
                        NarcFile narcFile = narc.getFileByPath(joinPath(splitPath, i + 1));
                        if(narcFile != null){
                            return narcFile.getData();
                        }else{
                            throw new IOException("NARC internal file not found Exception");
                        }
                    }catch(Exception ex){
                        throw new IOException("File and NARC not found Exception");
                    }
                }
            }
            throw new IOException("File and NARC not found Exception");
        }
    }

    private static String joinPath(String[] splitPath, int startIndex) {
        String path = "";
        for (int i = startIndex; i < splitPath.length; i++) {
            if (!path.equals("")) {
                path += "/";
            }
            path += splitPath[i];
        }
        return path;
    }

    private static boolean isNarc(String filePath) {
        try {
            byte[] data = Files.readAllBytes(new File(filePath).toPath());
            return BinaryReader.readString(data, 0, 4).equals("NARC");
        }catch(Exception ex){
            return false;
        }
    }

}

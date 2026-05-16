package Gastly.fr.mclauncher;

import java.io.File;

public class Locations
{
	static String appData = System.getenv("APPDATA");
	public static final File rootFolder = new File(appData,".minecraft");
	public static final File librariesFolder = new File(rootFolder, "libraries");
	public static final File versionsFolder = new File(rootFolder, "FFversion/versions");
	public static final File assetsFolder = new File(rootFolder, "assets");
	public static final File assetsObjectsFolder = new File(assetsFolder, "objects"); //May not be renamed.
	public static final File assetsIndexesFolder = new File(assetsFolder, "indexes"); //May not be renamed, although "indices" is a better form.
	
	public static final File gameFolder189 = new File(rootFolder, "game-data/1.8.9-1.12.2"); //Test to move away the game files. More structure. And research.
	public static final File gameFolder113 = new File(rootFolder, "game-data/1.13.2-1.21"); //Test to move away the game files. More structure. And research.
	public static final File runFolder = new File(rootFolder, "game-run"); //Test to move away the game files. More structure. And research.
	public static final File resourcePacksFolder = new File(rootFolder,"resourcepacks");
	public static final File savesFolder = new File(rootFolder,"saves");

	public static final File JDKFolder = new File("C:/Users/natpe/.jdks");
	public static final File JavaPath = new File(JDKFolder, "graalvm-jdk-21.0.5");
}

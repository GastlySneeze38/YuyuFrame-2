package Gastly.fr.mclauncher.newdata;

import Gastly.fr.mclauncher.data.DownloadSection;
import Gastly.fr.mclauncher.data.RunArguments;
import Gastly.fr.mclauncher.data.assets.AssetsInfo;
import Gastly.fr.mclauncher.data.libraries.LibraryInfo;
import de.ecconia.java.json.JSONObject;

public abstract class LoadedVersion extends Version
{
	private String mainClass;
	
	public LoadedVersion(String type, String time, String releaseTime, String id, String mainClass)
	{
		super(type, time, releaseTime, id);
		this.mainClass = mainClass;
	}
	
	public LoadedVersion(JSONObject object)
	{
		super(object);
		this.mainClass = object.getString("mainClass");
	}
	
	public String getMainClass()
	{
		return mainClass;
	}
	
	public abstract DownloadSection getDownloads();
	
	public abstract AssetsInfo getAssetsInfo();
	
	public abstract LibraryInfo getLibraryInfo();
	
	public abstract RunArguments getArguments();
}

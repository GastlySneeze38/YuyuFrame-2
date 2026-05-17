package Gastly.fr.mclauncher.newdata;

import Gastly.fr.mclauncher.data.DownloadSection;
import Gastly.fr.mclauncher.data.RunArguments;
import Gastly.fr.mclauncher.data.RunArgumentsNew;
import Gastly.fr.mclauncher.data.assets.AssetsInfo;
import Gastly.fr.mclauncher.data.libraries.LibraryInfo;
import de.ecconia.java.json.JSONObject;

public class InheritingVersion extends LoadedVersion
{
	private final LoadedVersion parentVersion;
	private final LibraryInfo libraryInfo;
	
	public InheritingVersion(LoadedVersion parentVersion, JSONObject versionObject)
	{
		super(versionObject);
		this.parentVersion = parentVersion;
		
		this.libraryInfo = LibraryInfo.mergedCopy(parentVersion.getLibraryInfo(), new LibraryInfo(versionObject.getArray("libraries")));
		RunArgumentsNew arguments = new RunArgumentsNew(versionObject.getObject("arguments"));
		if(!arguments.isEmpty())
		{
			throw new RuntimeException("Arguments are not empty, please send the version.json to the developer.");
		}
	}
	
	@Override
	public DownloadSection getDownloads()
	{
		return parentVersion.getDownloads();
	}
	
	@Override
	public AssetsInfo getAssetsInfo()
	{
		return parentVersion.getAssetsInfo();
	}
	
	@Override
	public LibraryInfo getLibraryInfo()
	{
		return libraryInfo;
	}
	
	@Override
	public RunArguments getArguments()
	{
		return parentVersion.getArguments();
	}
}

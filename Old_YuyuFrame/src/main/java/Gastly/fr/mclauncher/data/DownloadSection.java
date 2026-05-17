package Gastly.fr.mclauncher.data;

import Gastly.fr.mclauncher.download.ArtifactDownload;
import de.ecconia.java.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DownloadSection
{
	private final Map<String, ArtifactDownload> downloads = new HashMap<>();
	
	public DownloadSection(JSONObject object)
	{
		for(Entry<String, Object> entry : object.getEntries().entrySet())
		{
			downloads.put(entry.getKey(), new ArtifactDownload(JSONObject.asObject(entry.getValue())));
		}
	}
	
	public ArtifactDownload get(String type)
	{
		return downloads.get(type);
	}
}

package Gastly.fr.mclauncher.download;

import java.io.File;

public interface DownloadInfo
{
	boolean download(File destination);
	
	boolean exists(File destination);
}

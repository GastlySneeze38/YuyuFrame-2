package Gastly.fr.mclauncher;

import Gastly.fr.mclauncher.download.DownloadInfo;
import java.io.File;

public class NoOpDownload implements DownloadInfo {
	@Override
	public boolean download(File destination) {
		// Ne fait rien et retourne true
		return true;
	}

	@Override
	public boolean exists(File destination) {
		// Retourne false, car il n'existe pas réellement
		return false;
	}
}

package Gastly.fr.mclauncher.data;

import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.newdata.LoadedVersion;

import java.util.Collection;

public interface RunArguments
{
	Collection<String> build(LoadedVersion version, String classpath, String absolutePath, DataBaseOfUser profile);
}

package Gastly.fr.mclauncher;

import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill.ChillLunchPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Gamer.GamerLunchPanel;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.SyncroniseLunchInfo;
import Gastly.fr.mclauncher.GraphicPart.PanelGraphic.ParameterPanel.LogicParameter;
import Gastly.fr.mclauncher.Login.DataBaseOfUser;
import Gastly.fr.mclauncher.data.TimeCount;
import Gastly.fr.mclauncher.newdata.LoadedVersion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MCLauncherLab {

	public static void run(LoadedVersion version, DataBaseOfUser profile) throws InterruptedException {
		Locations.gameFolder189.mkdirs();
		Locations.gameFolder113.mkdirs();
		Locations.runFolder.mkdirs();
		Locations.resourcePacksFolder.mkdirs();
		Locations.savesFolder.mkdirs();
		
		File versionFolder = new File(Locations.versionsFolder, version.getId());

		//Create classpath:
		String classpath = version.getLibraryInfo().genClasspath(Locations.librariesFolder);
		classpath += File.pathSeparator + new File(versionFolder, version.getId() + ".jar").getAbsolutePath();
		//classpath += File.pathSeparator + new File("libs/launchwrapper-1.12.jar").getAbsolutePath();

		//Create natives directory:
		File nativesFolder = new File(versionFolder, version.getId() + "-natives");
		
		List<String> arguments = new ArrayList<>();
		arguments.add("java");
		arguments.add("-Xmx"+ LogicParameter.Ram / 1024 +"G");
		arguments.add("-Xms"+ LogicParameter.Ram / 1024 +"G");
		arguments.add("-XX:+UnlockExperimentalVMOptions");
		arguments.add("-XX:+AlwaysPreTouch");
		arguments.add("-XX:+UseStringDeduplication");
		arguments.add("-XX:+OptimizeStringConcat");
		arguments.add("-XX:-UseAdaptiveSizePolicy");
		arguments.add("-XX:+DisableExplicitGC");
		arguments.addAll(version.getArguments().build(version, classpath, nativesFolder.getAbsolutePath(), profile));
		/*arguments.add("net.minecraft.launchwrapper.Launch");
		arguments.add("--tweakClass");
		arguments.add("de.ecconia.mclauncher.ModLoader.MainModLoader");*/

		for(int i = 0; i < arguments.size(); i++)
		{
			String argument = arguments.get(i);
			if(argument.indexOf(' ') != -1)
			{
				argument = '"' + argument + '"';
				arguments.set(i, argument);
			}
		}
		TimeCount time = new TimeCount();

		ProcessBuilder builder = new ProcessBuilder(arguments);
		if (SyncroniseLunchInfo.versionToLaunch.equals("1.8.9") || SyncroniseLunchInfo.versionToLaunch.equals("1.12.2")){
			builder.directory(Locations.gameFolder189);
		}else{
			builder.directory(Locations.gameFolder113);
		}
		try
		{
			Process process = builder.start();
			new Thread(() -> {
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String tmp;
				try
				{
					time.start();
					while((tmp = reader.readLine()) != null)
					{
						System.out.println(">> " + tmp);

						if (LogicParameter.Gamer){
							GamerLunchPanel.StartButton.setText("Started");
						}else{
							ChillLunchPanel.StartButton.setText("Started");
						}
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
            }).start();

			new Thread(() -> {

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				String tmp;
				try
				{
					while((tmp = reader.readLine()) != null)
					{
						System.out.println("x> " + tmp);
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}).start();

			process.waitFor();

			int timeCount = time.Stop();
			System.out.println(timeCount);

			if (LogicParameter.Gamer){
				GamerLunchPanel.StartButton.setText(SyncroniseLunchInfo.Wait_To_Lunch);
			}else{
				ChillLunchPanel.StartButton.setText(SyncroniseLunchInfo.Wait_To_Lunch);
			}
		}
		catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
    }
}

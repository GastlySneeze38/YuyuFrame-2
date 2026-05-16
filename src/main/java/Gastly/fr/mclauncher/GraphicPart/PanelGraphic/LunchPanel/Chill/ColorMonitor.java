package Gastly.fr.mclauncher.GraphicPart.PanelGraphic.LunchPanel.Chill;

import java.awt.*;

public class ColorMonitor {

    public static Color Section1;
    public static Color Section2;
    public static Color Section3;

    public static Color clairSection1;
    public static Color clairSection2;
    public static Color clairSection3;

    public static Color SombreSection1;
    public static Color SombreSection2;
    public static Color SombreSection3;

    public ColorMonitor(int padlet){
        if (padlet == 1){
            getLoad1padlet();
        } else if (padlet == 2) {
            getLoad2padlet();
        } else if (padlet == 3) {
            getLoad3padlet();
        }
    }

    public static void getLoad1padlet(){
        Section1 = new Color(243, 33, 51);
        Section2 = new Color(199, 149, 107);
        Section3 = new Color(204, 208, 172);

        clairSection1 = new Color(243, 73, 51);
        clairSection2 = new Color(199, 199, 107);
        clairSection3 = new Color(204, 248, 172);

        SombreSection1 = new Color(243, 3, 51);
        SombreSection2 = new Color(199, 109, 107);
        SombreSection3 = new Color(204, 168, 172);
    }

    public static void getLoad2padlet(){
        Section1 = new Color(2, 193, 64);
        Section2 = new Color(0, 169, 158);
        Section3 = new Color(1, 133, 207);

        clairSection1 = new Color(2, 213, 64);
        clairSection2 = new Color(0, 209, 158);
        clairSection3 = new Color(1, 173, 207);

        SombreSection1 = new Color(2, 143, 64);
        SombreSection2 = new Color(0, 129, 158);
        SombreSection3 = new Color(1, 103, 207);
    }

    public static void getLoad3padlet(){
        Section1 = new Color(2, 100, 64);
        Section2 = new Color(0, 100, 158);
        Section3 = new Color(1, 50, 207);

        clairSection1 = new Color(2, 140, 64);
        clairSection2 = new Color(0, 140, 158);
        clairSection3 = new Color(1, 90, 207);

        SombreSection1 = new Color(2, 60, 64);
        SombreSection2 = new Color(0, 70, 158);
        SombreSection3 = new Color(1, 10, 207);
    }

}

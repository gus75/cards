package ru.rik.cardsnew.config;

public class Settings {
	public static volatile String MSK_REGCODE = "77";
    public static volatile int ASR_AFFECTED = 10;
    public static volatile int ACD_AFFECTED = 10;

    public static volatile int NORM_INTERVAL = 190;
    
    public static volatile int FAIL_INTERVAL = 20;

    public static volatile String FAKE_BANK_IP = "192.168.99.99";
    public static volatile String FAKE_CARD_PLACE = "b0000099";
    // max time for changing card and rebooting Sec
    public static volatile int TIME_FOR_SWITCH = 300;
    
    
//  @Setting(description = "MTS statistics window (minutes)")
//  public static volatile int MTS_WINDOW = 3840;
//  @Setting(description = "Beeline statistics window (minutes)")
//  public static volatile int BEELINE_WINDOW = 5760;
//  @Setting(description = "Megafon statistics window (minutes)")
//  public static volatile int MEGAFON_WINDOW = 5760;
//  @Setting(description = "Максимальное коли-во свежих event в очереди при удалении")
//  public static volatile int STOP_NUMBER = 5;
//"Interval between normal checking Channel's GSM Status. Sec")
	
	public Settings() {
	}

}

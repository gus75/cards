package ru.rik.cardsnew.service.telnet;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Assert;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rik.cardsnew.domain.Box;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.Channel;
import ru.rik.cardsnew.domain.Oper;
import ru.rik.cardsnew.domain.State;
import ru.rik.cardsnew.domain.Ussd;
import ru.rik.cardsnew.domain.Ussd.UssdBuilder;
import ru.rik.cardsnew.service.TaskDescr;
@NoArgsConstructor
public class UssdTask implements State{
	@Getter @Setter private Channel ch;
	@Getter @Setter private Card card;
	@Getter @Setter private TelnetClient telnetClient;
	@Getter @Setter private String encodedResp;
	@Setter private String decodedResp;
	@Getter @Setter private TaskDescr td;
	
	private static final Pattern MSG_PATTERN = 
			Pattern.compile("^\\+CUSD:\\s+(\\d)(?:,\\s*\"([^\"]*))?(?:\",\\s*(\\d+)\\s*)?\"?\r?$"
					, Pattern.MULTILINE);
	private static final Pattern pd = Pattern.compile("(\\d)");
	private static final Pattern ps = Pattern.compile("(\\*)");
	private static final Pattern pp = Pattern.compile("(\\#)");
	
	private static final Pattern greenBalance = Pattern.compile("(^\\-*\\d{1,4}[.,]\\d\\d)(р.*)", Pattern.MULTILINE);
	private static final Pattern yellowRedBalance = 
			Pattern.compile("^\\s*(Баланс.?|Минус.?|Balans.?|Balance.?|Minus.?|\\-)\\s*(-*\\d{1,4}[.,]*\\d{0,2})(\\s*р*.*)"
					, Pattern.MULTILINE);
	private static final Pattern smsNeeded = Pattern.compile("^.*SMS.*$", Pattern.MULTILINE);
	
	public UssdTask(Channel ch, Card card, TelnetClient telnetClient, TaskDescr td) {
		super();
		this.ch = ch;
		this.card = card;
		this.telnetClient = telnetClient;
		this.td = td;
	}

	
	public static UssdTask get(TelnetHelper telnetHelper, Channel ch, Card card, String request, TaskDescr td) throws SocketException, IOException {
		Assert.assertNotNull(ch);
		Assert.assertNotNull(card);
		td.setStage("Connecting to " + ch.getBox().getIp() + ":" + ch.getLine().getTelnetport());
		TelnetClient tc  = telnetHelper.getConnection(ch.getBox().getIp() ,
				ch.getLine().getTelnetport(),
				Box.DEF_USER, Box.DEF_PASSWORD);
		UssdTask task = new UssdTask(ch, card, tc, td);
		
		td.setStage("Sending ussd... " + request);
		String encodedResp;
		try {
			encodedResp = telnetHelper.sendUssd(tc, ch.getLine().getNport() + 1, task.encodeRequest(request));
			task.setEncodedResp(encodedResp);
			return task;
		} catch (Exception e) {
			throw e;
		} finally {
			telnetHelper.disconnect(tc);
		}
	}
	
	
	public String getDecodedResponse() throws IllegalStateException {
		if (encodedResp == null)
			throw new IllegalStateException("there isn't a valid response on ussd request "  + ch.getName() + " " + card.getName());
		String str = null;
		Matcher m = MSG_PATTERN.matcher(encodedResp);
		if (m.find()) {
			str = m.group(2);
		} 
		if (str == null) 
			throw new IllegalStateException("can't parse ussd response for " + ch.getName() + " " + card.getName() + " "+ encodedResp);
		
		byte[] responded = pduToBytes(str);
		return decodeUcs2Encoding(null, responded);
	}	
		
	
	
	public String encodeRequest(String req) {
		String result = null;
		Matcher m = pd.matcher(req);
		if (m.find())  result = m.replaceAll("003$1");
		
		m = ps.matcher(result);
		if (m.find())  result = m.replaceAll("002a");

		m = pp.matcher(result);
		if (m.find())  result = m.replaceAll("0023");

		return result;
	}
	
	/** Parses response and returns USSD.       */
	public Ussd getUssd() {
		if (decodedResp == null)
			decodedResp = getDecodedResponse();
		Float balance = null;
		String str = null;
		UssdBuilder b = Ussd.builder().card(card)
				.date(new Date())
				.decodedmsg(decodedResp);
		if (ch.getGroup().getOper() == Oper.GREEN) {
			Matcher m = greenBalance.matcher(decodedResp);
			if (m.find())  
				str = m.group(1);
			else if (isSmsNeeded()) 
				return b.smsNeeded(true).build();
		} else {
			Matcher m = yellowRedBalance.matcher(decodedResp);
			if (m.find()) {
				str = m.group(2);
				if (m.group(1).contains("Минус") || m.group(1).contains("Minus") || m.group(1).contains("-"))
					str = "-"+str;
			} else if (isSmsNeeded()) 
				return b.smsNeeded(true).build();
		}
		if (str != null)
			balance = Float.parseFloat(str.replace(',', '.'));
		
		return b.smsNeeded(false).balance(balance).build();
	}
	
	
	private boolean isSmsNeeded() {
		Matcher m = smsNeeded.matcher(decodedResp);
		return m.find();  
	}
 	
	
	// converts a PDU string to a byte array
		private byte[] pduToBytes(String s) {
			byte[] bytes = new byte[s.length() / 2];
			for (int i = 0; i < s.length(); i += 2) {
				bytes[i / 2] = (byte) (Integer.parseInt(s.substring(i, i + 2), 16));
			}
			return bytes;
		}

		
		private String decodeUcs2Encoding(byte[] udhData, byte[] pduData) {
			try {
				int udhLength = ((udhData == null) ? 0 : udhData.length);
				return new String(pduData, udhLength, pduData.length - udhLength, "UTF-16");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}


		@Override public long getId() {return ch.getId();	}
		@Override public void setId(long id) {}

		@Override public String getName() {return ch.getName();}
		@Override public void setName(String name) {}

		@Override public Class<?> getClazz() {return SmsTask.class;}
}

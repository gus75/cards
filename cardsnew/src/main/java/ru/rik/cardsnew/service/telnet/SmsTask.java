package ru.rik.cardsnew.service.telnet;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Builder;
import ru.rik.cardsnew.domain.Box;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.Channel;
import ru.rik.cardsnew.domain.Line;
import ru.rik.cardsnew.domain.MyState;
import ru.rik.cardsnew.domain.Sms;

public class SmsTask implements MyState {
	private static final Logger logger = LoggerFactory.getLogger(SmsTask.class);		

	@Getter private final Channel ch;
	@Getter private final Card card;
	@Getter private final Channel pair;
	@Getter private final Card pairCard;
	@Getter @Setter private List<Sms> smslist;
	@Getter @Setter private List<Sms> pairSmslist;
	@Getter @Setter private TelnetClient telnetClient;
	@Getter @Setter private Phase phase; 
	
	
	@Builder
	public SmsTask(Channel ch, Card card, Channel pair, Card pairCard, TelnetClient telnetClient, List<Sms> allsms, Phase phase) {
		if (ch == null || card == null)
			throw new IllegalArgumentException("channel or cards can not be null!");
		this.ch = ch;
		this.card = card;
		this.pairCard = pairCard;
		this.pair = pair;
		this.telnetClient = telnetClient;
		smslist = allsms;
		this.phase = phase;
	}
	

	public static SmsTask get(TelnetHelper h, Channel ch, Card card, Channel pair, Card pairCard) throws SocketException, IOException {
		TelnetClient tc  = h.getConnection(ch.getBox().getIp() ,
				ch.getLine().getTelnetport(),
				Box.DEF_USER, Box.DEF_PASSWORD);
		
		List<Sms> allsms = h.FetchSmsFromChannel(tc, ch.getLine().getNport() + 1);
		System.out.println("FetchMain " + ch.getName());
		SmsTask smstask = SmsTask.builder()
			.ch(ch).pair(pair)
			.card(card).pairCard(pairCard)
			.telnetClient(tc)
			.allsms(allsms)
			.phase(Phase.FetchMain)
			.build();
		smstask.getSmslist().stream().forEach(s -> {s.setChannel(ch);	s.setCard(card);});
		return smstask;
	}

	
	public SmsTask deleteMain(TelnetHelper h) {
		phase = Phase.DeleteMain;
		System.out.println(phase + " " + ch.getName());
		h.deleteSms(telnetClient, smslist);
		return this;
	}
	
	
	public SmsTask fetchPair(TelnetHelper h) {
		phase = Phase.FetchPair;
		System.out.println(phase + " " + ch.getName());
		if (pair != null)
			pairSmslist = h.FetchSmsFromChannel(telnetClient, pair.getLine().getNport() + 1);
		pairSmslist.stream().forEach(s -> {s.setChannel(pair);	s.setCard(pairCard);});
		return this;
	}
	
	
	public SmsTask deletePair(TelnetHelper h){
		phase = Phase.DeletePair;
		System.out.println(phase + " " + ch.getName());
		if (pair != null)
			h.deleteSms(telnetClient, pairSmslist);
		disconnect();
		return this;
	}
	
	public void disconnect() {
		try {
			System.out.println("doing disconnect " + telnetClient);
			telnetClient.disconnect();
		} catch (IOException e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	@Override public long getId() {return ch.getId();	}
	@Override public void setId(long id) {}

	@Override public String getName() {return ch.getName();}
	@Override public void setName(String name) {}

	@Override public Class<?> getClazz() {return SmsTask.class;}


	public enum Phase {
		FetchMain, DeleteMain, FetchPair, DeletePair; 
	}
	

	public static void main(String[] args) throws SocketException, IOException {
		TelnetHelper h = new TelnetHelperImpl();
		Channel ch = Channel.builder()
				.box(Box.builder().ip("192.168.5.102").build())
				.line(Line.L1)
				.build();
		
		
		Channel pair = Channel.builder()
				.box(Box.builder().ip("192.168.5.102").build())
				.line(Line.L2)
				.build();
		
		SmsTask task = get(h, ch, null, pair, null);
		System.out.println(task.getSmslist());
		if (task.phase == Phase.FetchMain)
			task = task.deleteMain(h);
		
		if (task.phase == Phase.DeleteMain)
			task.fetchPair(h);
		
		if (task.phase == Phase.DeletePair)
			task.deletePair(h);
		
		System.out.println(task.getSmslist());

		
		

	}


//	@Override
//	public String toString() {
//		return "SmsTask [ch=" + ch.getName() + ", card=" + card.getName() + ", pair=" + pair.getName() 
//		+ ", pairCard=" + pairCard.getName() + ", smslist="
//				+ smslist + ", pairSmslist=" + pairSmslist + ", telnetClient=" + telnetClient + ", phase=" + phase
//				+ "]";
//	}
}
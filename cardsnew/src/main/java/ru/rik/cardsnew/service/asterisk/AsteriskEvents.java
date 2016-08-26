package ru.rik.cardsnew.service.asterisk;

import java.io.IOException;
import java.text.ParseException;

import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.event.CdrEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.rik.cardsnew.db.CardRepoImpl;
import ru.rik.cardsnew.db.ChannelRepoImpl;
import ru.rik.cardsnew.db.GenericRepoImpl;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.Channel;
import ru.rik.cardsnew.domain.events.Cdr;
import ru.rik.cardsnew.domain.repo.CardsStates;
import ru.rik.cardsnew.domain.repo.Cdrs;

public class AsteriskEvents implements ManagerEventListener {
	static final Logger logger = LoggerFactory.getLogger(GenericRepoImpl.class);

	private ManagerConnection managerConnection;
	
	@Autowired private CardRepoImpl cardRepo;
	@Autowired private CardsStates cardsStates;
	@Autowired private ChannelRepoImpl chanRepo;
	@Autowired private Cdrs cdrs;

	public AsteriskEvents() {
		ManagerConnectionFactory factory = new ManagerConnectionFactory("localhost", "myasterisk", "mycode");
		this.managerConnection = factory.createManagerConnection();
	}

	public void start() throws IOException, AuthenticationFailedException, TimeoutException, InterruptedException {
		logger.info("Asterisk managerConnection log in ");

		managerConnection.addEventListener(this);
		managerConnection.login();
	}

	public void stop() throws IllegalStateException {
		logger.info("Asterisk managerConnection is logging off ");
		managerConnection.logoff();
	}

	public void onManagerEvent(ManagerEvent event) {
		if (event instanceof CdrEvent) {
			CdrEvent cdrevent = (CdrEvent) event;
			if (cdrevent.getUserField() != null) {
				logger.debug("AnswerTime: " + cdrevent.getAnswerTime() + " AnswerTimeAsDate: "
						+ cdrevent.getAnswerTimeAsDate() + " StartTime: " + cdrevent.getStartTime()
						+ " StartTimeAsDate: " + cdrevent.getStartTimeAsDate() + " Src: " + cdrevent.getSrc()
						+ " Destination: " + cdrevent.getDestination() + " Disposition: " + cdrevent.getDisposition()
						+ " BillableSeconds: " + cdrevent.getBillableSeconds() + " Duration: " + cdrevent.getDuration()
						+ " UserField: " + cdrevent.getUserField() + " Trunk: " + cdrevent.getTrunk() + " gateip: "
						+ cdrevent.getGateip() + " Regcode: " + cdrevent.getRegcode());
				addCdr(cdrevent);
			}
		}

	}

	public void addCdr(CdrEvent ce) {
		String cardname = ce.getUserField();
		if (cardname == null ) return;
		
		Card card = cardRepo.findByName(cardname);
		Channel chan = chanRepo.findByName(Cdr.parseChannel(ce.getDestinationChannel()));
		
		if (card == null) { // FIXME to add null checking for channel
			logger.debug("The card with name " + cardname + " does not exist");
			return;
		}
		try {
			Cdr cdr = Cdr.builder()
				.date(ce.getStartTime())
				.src(ce.getSrc())
				.dst(ce.getDestination())
				.cardId(card.getId())
				.billsec(ce.getBillableSeconds())
				.trunk(ce.getTrunk())
				.disp(ce.getDisposition())
				.regcode(ce.getRegcode())
				.uniqueid(ce.getUniqueId())
				.channelId(chan.getId())
				.build();
			
		cdrs.addCdr(cdr);
		} catch (ParseException pe) {
			logger.error("can not create CdrEvent calldate: " + ce.getStartTime() + " cardname: " + cardname, pe);
		}			
	}
}
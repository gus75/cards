package ru.rik.cardsnew.db;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ru.rik.cardsnew.domain.Box;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.CardStat;
import ru.rik.cardsnew.domain.Channel;
import ru.rik.cardsnew.domain.ChannelState;
import ru.rik.cardsnew.domain.ChannelState.Status;
import ru.rik.cardsnew.domain.Grp;
import ru.rik.cardsnew.domain.Route;
import ru.rik.cardsnew.domain.Sms;
import ru.rik.cardsnew.domain.Trunk;
import ru.rik.cardsnew.domain.Ussd;

@Repository
public class ChannelRepoImpl extends GenericRepoImpl<Channel, ChannelState> implements ChannelRepo {
	static final Logger logger = LoggerFactory.getLogger(ChannelRepoImpl.class);
	@Autowired private CardRepo cards;
	
	public ChannelRepoImpl() {
		super(Channel.class, ChannelState.class);
	}
	
	@PostConstruct 
	@Override
	public void init() {
		super.init();
		logger.debug("initializing static variable");
	}

	@Override
	public Channel findPair(Channel ch) {
		try {
			return em.createNamedQuery("findPair", Channel.class)
					.setParameter("box", ch.getBox())
					.setParameter("line", ch.getLine().getPair())
					.setHint("org.hibernate.cacheable", true)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
		
	@Override
	public List<Channel> getSorted(Trunk t, Route route)  {
		List<Channel> result = t.getChannels().stream()
				.filter(ch -> ch.isEnabled() 
					&& ch.getState(this).getStatus() == Status.Ready 
					&& ch.getCard() != null  
					&& ch.getCard().getStat(cards).getMinRemains(route) > 0)
//				.peek(ch -> System.out.println(ch.getState() ))
				.sorted((ch1, ch2) -> Long.compare(ch1.getId(), ch2.getId()))					
				.sorted((ch1, ch2) -> Integer.compare(ch1.getState(this).getPriority(), ch2.getState(this).getPriority()))
				.collect(Collectors.toList());
		
		if (result.size() > 0)
			result.get(0).getState(this).incPriority();
		return result;	
	}
	
	@Override
	public List<Channel> findGroupChans(Grp grp) {
		return em.createNamedQuery("findByGrp", Channel.class)
				.setParameter("grp", grp)
				.setHint("org.hibernate.cacheable", true)
				.getResultList();
	}
    	
	@Override
	public List<Channel> findBoxChans(Box box) {
		return em.createNamedQuery("findByBox", Channel.class)
				.setParameter("box", box)
				.setHint("org.hibernate.cacheable", true)
				.getResultList();
	}	

	/** Transactional.
	 * Fixes changing card in channel into database, set the channel's Status Inchange and peer's - PeerInchange
	 * @throws ConcurrentModificationException 
	 */
	@Override @Transactional
//	(propagation = Propagation.REQUIRES_NEW)
	public Channel switchCard(Channel ch, Card c) {
		Assert.notNull(ch);
		Channel chan = findById(ch.getId());
		if (chan.getVersion() != ch.getVersion())
			throw new ConcurrentModificationException("Channel " + chan.getName() + " was modified");	
		chan.getState(this).setStatus(Status.Inchange);

		Channel peer = chan.getPair(this);
		if (peer!=null)
			peer.getState(this).setStatus(Status.PeerInchange);
		
		Card oldCard = chan.getCard();
		if (oldCard != null) {
			oldCard.setChannelId(0); // set to null channel Id
			CardStat st = oldCard.getStat(cards);
			if (!st.setFree(false, true))
				logger.error("card {} was free, but expected not free", oldCard.getName());
		}	
		
		if (c != null) {
			Card newCard = cards.findById(c.getId());
			if (newCard.getVersion() != c.getVersion())
				throw new ConcurrentModificationException("Card " + newCard.getName() + " was modified");	

			newCard.setChannelId(chan.getId());
			chan.setCard(newCard);
		} else 
			chan.setCard(c);

		return makePersistent(chan);
	}
	
	@Autowired EventRepo events;
	@Override @Transactional
	public void smsHandle(List<Sms> list) {
		for (Sms sms: list) {
			em.merge(sms);
			
			Ussd b = sms.getBalance();
			if (b != null) {
				logger.debug("This is balance: " + sms.toString() + " balance: " + b.toString());
				events.save(b);
				CardStat cs = b.getCard().getStat(cards);
				cs.applyBalance(b);
			} else 
				logger.debug(sms.toString());
		}	
	}
	
	@Override @Transactional
	public void setCardToNull(List<Channel> list) {
		list.stream()
		.filter(ch -> ch.isEnabled())
		.peek(ch-> ch.setCard(null))
		.forEach(ch-> makePersistent(ch));
	}
	
	@Override
	public ChannelState getPairsState(long id) {
		Channel ch = findById(id);
		Channel pair= ch.getPair(this);
		System.out.println("Pair : " + pair);
		if (pair == null)
			return null;
		return findStateById(pair.getId());
	}
}

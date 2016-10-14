package ru.rik.cardsnew.db;

import java.util.List;

import ru.rik.cardsnew.domain.Box;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.Channel;
import ru.rik.cardsnew.domain.ChannelState;
import ru.rik.cardsnew.domain.Grp;
import ru.rik.cardsnew.domain.Trunk;

public interface ChannelRepo extends GenericRepo<Channel, ChannelState>{

	Channel findPair(Channel ch);

	List<Channel> getSorted(Trunk t) throws NullPointerException;

	List<Channel> findGroupChans(Grp grp);

	List<Channel> findBoxChans(Box box);

	void switchCard(Channel chan, Card c);

}
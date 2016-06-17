package ru.rik.cardsnew;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.rik.cardsnew.config.RootConfig;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.repo.Cards;
import ru.rik.cardsnew.domain.repo.Channels;
import ru.rik.cardsnew.domain.repo.Grps;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={RootConfig.class})
public class InitTest {

	@Autowired 	Cards cards;
	@Autowired 	Channels channels;
	@Autowired 	Grps grps;
//	@Before
	@Test
	public void test() {
		System.out.println("============ Cards =============");
		for (Card c : cards.getMap().values()) {
			System.out.println(c.toStringAll());
		}
//		for (Channel c : channels.getMap().values()) {
//			System.out.println(c.toString());
//		}
//		for (Grp c : grps.getMap().values()) {
//			System.out.println(c.toString() + " channel: " + c.getChannels().hashCode());
//		}
	}
	
	@Test
	public void testChangeCard() {
		
	}
	
}

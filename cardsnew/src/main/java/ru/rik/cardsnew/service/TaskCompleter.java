package ru.rik.cardsnew.service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
import ru.rik.cardsnew.db.BankRepo;
import ru.rik.cardsnew.db.CardRepo;
import ru.rik.cardsnew.db.ChannelRepo;
import ru.rik.cardsnew.domain.Balance;
import ru.rik.cardsnew.domain.BankState;
import ru.rik.cardsnew.domain.Card;
import ru.rik.cardsnew.domain.CardStat;
import ru.rik.cardsnew.domain.Channel;
import ru.rik.cardsnew.domain.ChannelState;
import ru.rik.cardsnew.domain.ChannelState.Status;
import ru.rik.cardsnew.domain.State;
import ru.rik.cardsnew.domain.Util;
import ru.rik.cardsnew.service.http.BankStatus;
import ru.rik.cardsnew.service.http.GsmState;
import ru.rik.cardsnew.service.http.SimSet;
import ru.rik.cardsnew.service.telnet.SmsTask;
import ru.rik.cardsnew.service.telnet.TelnetHelper;
import ru.rik.cardsnew.service.telnet.UssdTask;

public class TaskCompleter implements Runnable{
	private static final Logger logger = LoggerFactory.getLogger(TaskCompleter.class);		

	private final CompletionService<State> completionServ;
	private final ThreadPoolTaskExecutor executor;
	@Getter private final ConcurrentMap<Future<State>, TaskDescr> map;
	@Autowired @Setter private ChannelRepo chans;
	@Autowired private CardRepo cards;
	@Autowired private BankRepo banks;
	@Autowired @Setter private TelnetHelper telnetHandler;

	@Autowired
	public TaskCompleter(CompletionService<State> completionService, ThreadPoolTaskExecutor taskExecutor ) {
		this.completionServ = completionService;
		this.executor = taskExecutor;
		this.map = new ConcurrentHashMap<>();
	}

	
	public void start() {
		executor.submit(this);
	}

	public Future<State> addTask(Callable<State> task, TaskDescr descr ) {
		if (descr.getStage() == null)
			descr.setStage("queued");
		
		Future<State> f = completionServ.submit(task);
		
		map.putIfAbsent(f, descr);
		return f;
	}
	

	
	@Override
	public void run() {
		Future<State> f = null;
		while (!Thread.currentThread().isInterrupted()) {
			try {
				f = completionServ.take();
				State result = f.get();
				TaskDescr td = map.remove(f);
				
				if (result.getClazz() == GsmState.class)				
					applyGsmState((GsmState) result);
				else if (result.getClazz() == SimSet.class) {
					applySimSet(result);
				} 
				else if (result.getClazz() == BankStatus.class) {
					applyBankStatus((BankStatus) result);
				} 
				else if (result.getClazz() == SwitchTask.class) {
					SwitchTask sw = (SwitchTask) result;
					logger.debug("installing in channel {} card {}", sw.getName(), sw.getCardName());
				}
				else if (td.getClazz() == SmsTask.class) {
					handleSms((SmsTask) result);
				} else if (td.getClazz() == UssdTask.class) {
					handleUssd((UssdTask) result);
				}
			} catch (InterruptedException e) {
				logger.info("interrupted");
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				execExceptionHandler(f, e);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}


	/**
     * Handles all exceptions during the task's execution
     */
	private void execExceptionHandler(Future<State> f, ExecutionException e) {
		try {
			Throwable cause = e.getCause();
			if (cause instanceof SocketTimeoutException || cause instanceof ConnectException) {
				TaskDescr descr = map.remove(f);
				State st = descr.getState();
				Class<?> task = descr.getClazz();
				
				if (task == SmsTask.class || task == UssdTask.class) {
					((ChannelState) st).setStatus(Status.Ready);
					Channel ch = chans.findById(st.getId());
//					logger.debug("{} channel {} {} {} task {} ", e.getMessage(), st.getName(), ch.getBox().getIp(), 
//							ch.getLine().getTelnetport(), task.getSimpleName());
				} 
				else if (task == GsmState.class || task == SimSet.class) 
						((ChannelState) st).setStatus(Status.Unreach);
				
				else if (task == BankState.class) {
					BankState bState = (BankState) st;
					bState.setAvailable(false);
					logger.error("bank {} is unavailable ", bState.getName());
				} 
				else
					logger.error(e.getMessage(), e);
			} else 
				logger.error(e.getMessage(), e);
		} catch (Exception e1) {
			logger.error(e1.getMessage(), e1);

		}
	}
	
	
	private void applyGsmState(GsmState g) {
		ChannelState st = chans.findStateById(g.getId());
		st.applyGsmStatus(g);
	}
	

	private void applySimSet(State result) {
		SimSet ss = (SimSet) result;
		ChannelState st = chans.findStateById(ss.getId());
		Assert.notNull(st, "SimSet applying. Can not find ChanelState " + ss.getName());
		st.applySimSet(ss);
		
		SimSet ssPair = ss.getPairData();
		if (ssPair != null) {
			ChannelState stPair = chans.findStateById(ssPair.getId());
			Assert.notNull(stPair, "SimSet applying. Can not find ChanelState " + ssPair.getName());
			stPair.applySimSet(ssPair);
		}
	}
	
	
	private void applyBankStatus(BankStatus g) {		
		BankState st = banks.findStateById(g.getId());
		st.applyBankStatus(g);
	}
	
	
	protected void handleSms(SmsTask smsTask) {
		Channel ch = smsTask.getCh();
		Channel pair = smsTask.getPair();
		ChannelState st = ch.getState(chans);
		TaskDescr descr = smsTask.getTd();
		ChannelState pairSt = pair != null ? pair.getState(chans) : null;
		switch (smsTask.getPhase()) {
		case FetchMain:
			st.setLastSmsFetchDate(new Date());
			if (smsTask.getSmslist().size() > 0 && smsTask.getCard() != null) {
				chans.smsHandle(smsTask.getSmslist());
				descr.setStage("queued for DeleteMain");
				addTask(() -> smsTask.deleteMain(telnetHandler), descr);
			} else if (smsTask.getPair() != null) {
				descr.setStage("queued for Fetch Pair");
				addTask(() -> smsTask.fetchPair(telnetHandler), descr);
			} else {
				st.smsSuccessfullyFetched();
				if (pairSt != null) pairSt.smsSuccessfullyFetched();
				smsTask.disconnect();
			}	
			break;
		case DeleteMain:	
			if (smsTask.getPair() != null) {
				descr.setStage("queued for Fetch Pair");
				addTask(() -> smsTask.fetchPair(telnetHandler), descr);
			} else {
				st.smsSuccessfullyFetched();
				if (pairSt != null) pairSt.smsSuccessfullyFetched();
				smsTask.disconnect();
			}	
			break;
		case FetchPair:	
			pairSt.setLastSmsFetchDate(new Date());
			if (smsTask.getPairSmslist().size() > 0 && smsTask.getPairCard() != null) { 
				chans.smsHandle(smsTask.getPairSmslist());
				descr.setStage("queued for Delete Pair");
				addTask(() -> smsTask.deletePair(telnetHandler), descr);
			} else {
				st.smsSuccessfullyFetched();
				if (pairSt != null) pairSt.smsSuccessfullyFetched();
				smsTask.disconnect();
			}	
			break;
		case DeletePair:	
			st.smsSuccessfullyFetched();
			if (pairSt != null) pairSt.smsSuccessfullyFetched();
			break;
		default:
			break;
		}
	}
	
	
	protected void handleUssd(UssdTask ussdTask) {
		Channel ch = ussdTask.getCh();
		ChannelState st = ch.getState(chans);
		Card card = ussdTask.getCard();
		CardStat cs = card.getStat(cards);
		
		Balance b = ussdTask.getBalance();
		
		if (b == null)
			throw new IllegalStateException("handling ussd answer " + ussdTask.getTd().toString() + " Balance is null!");
		logger.debug("!!! got ussd " + b.toString());
		if (b.isSmsNeeded()) {
			st.setStatus(Status.Smsfetch);
			st.setNextSmsFetchDate(Util.getNowPlusSec(60));
			//waiting for getting balance in Sms and setting next time check in 1 hour
			cs.setLastBalanceChecked(b.getDate());
			Date next = new Date(cs.getLastBalanceChecked().getTime() + 60 * 60 * 1000);
			cs.setNextBalanceCheck(next);
		} else {
			cs.applyBalance(b);
			cards.balanceSave(b);
			st.setStatus(Status.Ready);
		}
	
	}



	
}

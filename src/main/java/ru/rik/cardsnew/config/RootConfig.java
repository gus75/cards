package ru.rik.cardsnew.config;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ru.rik.cardsnew.domain.State;
import ru.rik.cardsnew.domain.repo.CardsStates;
import ru.rik.cardsnew.domain.repo.Cdrs;
import ru.rik.cardsnew.service.AsyncTasks;
import ru.rik.cardsnew.service.PeriodicTasks;
import ru.rik.cardsnew.service.SwitchTask;
import ru.rik.cardsnew.service.TaskCompleter;
import ru.rik.cardsnew.service.asterisk.AsteriskEvents;
import ru.rik.cardsnew.service.asterisk.CheckCDRTask;
import ru.rik.cardsnew.service.http.HttpHelper;
import ru.rik.cardsnew.service.telnet.TelnetHelper;
import ru.rik.cardsnew.service.telnet.TelnetHelperImpl;

@Configuration
@EnableCaching
@EnableScheduling
@EnableTransactionManagement

// @Import(ru.rik.cardsnew.db.JpaConfig.class)
@ComponentScan(basePackages = { "ru.rik.cardsnew.db" })

public class RootConfig implements SchedulingConfigurer {

	public static class WebPackage extends RegexPatternTypeFilter {
		public WebPackage() {
			super(Pattern.compile("ru.rik.cardsnew\\.web"));
		}
	}

	@Bean
	public AppInitializer appInitializer() {return new AppInitializer();}

	@Bean
	public CardsStates cardsStates() {return new CardsStates();}

	@Bean
	public HttpHelper httpHelper() {return new HttpHelper();}

	@Bean
	public PeriodicTasks periodicTasks() {return new PeriodicTasks();}

	@Bean
	public AsyncTasks asyncTasks() {return new AsyncTasks();}

	@Bean
	(initMethod = "init")
	public Cdrs cdrs() {return new Cdrs();}
	
	@Bean public TelnetHelper telnetHelper() {return new TelnetHelperImpl();}

	 @Bean 
//	 (initMethod="init")
	 public CheckCDRTask checkCDRTask() {return new CheckCDRTask();}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public AsteriskEvents asteriskEvents() {return new AsteriskEvents();}
	
	@Bean public SwitchTask switcher() {return new SwitchTask();}

	@Bean
	MyThreadFactory threadFactory() {return new MyThreadFactory();}

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(8);
//		executor.setMaxPoolSize(30);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("MyExecutor-");
		executor.setThreadFactory(threadFactory());
		executor.initialize();
		return executor;
	}

//	@Bean
//	public ThreadPoolExecutor executor() {
//		ThreadPoolExecutor e = new ThreadPoolExecutor(5, 15, 10, TimeUnit.SECONDS, null);
//		return e;
//	}
	
	@Bean(destroyMethod = "shutdown")
	public TaskExecutor taskSheduleExecutor() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("MyScheduler-");
		scheduler.setPoolSize(5);
		return scheduler;
	}

//	@Bean 
//	public BlockingQueue<Future<State>> blockingQueue() {
//		return new LinkedBlockingQueue<Future<State>>(1000);
//	}
	
	@Bean
	public CompletionService<State> completionService() {
		CompletionService<State> service = new ExecutorCompletionService<State>(taskExecutor());
		return service;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(taskSheduleExecutor());
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public TaskCompleter taskCompleter() {
		return new TaskCompleter(completionService(), taskExecutor());
	}

	// =================== Uncaught Exceptions Handler =====================

	public static class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		private static final Logger logger = LoggerFactory.getLogger(MyUncaughtExceptionHandler.class);
		private List<String> errors = new LinkedList<>();

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			String message = String.format("Thread %s hit by exception %s.", t.getName(), e.toString());
			logger.error(message);
			errors.add(message);
		}
	}

	public class MyThreadFactory implements ThreadFactory {
		private MyUncaughtExceptionHandler handler = new MyUncaughtExceptionHandler();

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setUncaughtExceptionHandler(handler);
			return t;
		}
	}
	
//	public class MyCompletionService<State> extends ExecutorCompletionService<State> {
//		private final BlockingQueue<Future<State>> completionQueue;
//		
//		public MyCompletionService(Executor executor, BlockingQueue<Future<State>> completionQueue) {
//			super(executor, completionQueue);
//			this.completionQueue = completionQueue;
//		}
//
//		public BlockingQueue<Future<State>> getCompletionQueue() {return completionQueue;}
//		
//	}
}

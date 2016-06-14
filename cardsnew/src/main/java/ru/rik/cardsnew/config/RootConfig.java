package ru.rik.cardsnew.config;

import java.sql.SQLException;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import ru.inlinetelecom.commons.settings.exceptions.SettingsException;
import ru.rik.cardsnew.config.RootConfig.WebPackage;
import ru.rik.cardsnew.domain.repo.Banks;
import ru.rik.cardsnew.domain.repo.Cards;

@Configuration
@Import(ru.rik.cardsnew.db.JpaConfig.class)
@ComponentScan(basePackages = { "ru.rik.cardsnew" }, excludeFilters = {
		@Filter(type = FilterType.CUSTOM, value = WebPackage.class) })

public class RootConfig {
	private static final Logger logger = LoggerFactory.getLogger(RootConfig.class);

	public static class WebPackage extends RegexPatternTypeFilter {
		public WebPackage() {
			super(Pattern.compile("ru.rik.cardsnew\\.web"));
		}
	}

	@Bean
	public Settings settings(DataSource dataSource) throws SettingsException, SQLException {
		return new Settings(dataSource);
	}
	
	@Bean 
	public Cards cards() {return new Cards();}
	
	@Bean 
	public Banks banks() {return new Banks();}
	
}

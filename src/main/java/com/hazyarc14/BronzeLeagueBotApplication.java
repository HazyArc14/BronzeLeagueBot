package com.hazyarc14;

import com.hazyarc14.listener.MessageListener;
import com.hazyarc14.listener.RankListener;
import com.hazyarc14.service.UserRankService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;

@SpringBootApplication
@EnableScheduling
public class BronzeLeagueBotApplication {

	@Autowired
	MessageListener messageListener;

	@Autowired
	RankListener rankListener;

	@Autowired
	UserRankService userRankService;

	public JDA jda;

	public static void main(String[] args) {
		SpringApplication.run(BronzeLeagueBotApplication.class, args);
	}

	@PostConstruct
	public void init() throws LoginException {

		String BOT_TOKEN = System.getenv("BOT_TOKEN");
		this.jda = JDABuilder
				.createDefault(BOT_TOKEN, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES)
				.addEventListeners(messageListener, rankListener).build();

	}

	@Scheduled(cron = "0 0 5 * * *")
	public void applyDecayToUserRanks() {
		userRankService.applyDecayToUserRanks(this.jda);
	}

	@Scheduled(fixedDelay = 60000)
	public void updateUserRanks() {
		userRankService.updateAllUserRanks(this.jda);
	}

}

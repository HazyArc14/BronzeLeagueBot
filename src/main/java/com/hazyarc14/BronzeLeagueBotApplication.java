package com.hazyarc14;

import com.hazyarc14.listener.ChannelListener;
import com.hazyarc14.listener.MessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;

@SpringBootApplication
@EnableScheduling
public class BronzeLeagueBotApplication {

	@Autowired
	MessageListener messageListener;

	@Autowired
	ChannelListener channelListener;

	public JDA jda;

	public static void main(String[] args) {
		SpringApplication.run(BronzeLeagueBotApplication.class, args);
	}

	@PostConstruct
	public void init() throws LoginException {

		String BOT_TOKEN = System.getenv("BOT_TOKEN");
		this.jda = JDABuilder
				.create(BOT_TOKEN, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.addEventListeners(messageListener, channelListener).build();

	}

}

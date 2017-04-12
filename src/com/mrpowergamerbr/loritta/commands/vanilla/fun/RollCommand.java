package com.mrpowergamerbr.loritta.commands.vanilla.fun;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.mrpowergamerbr.loritta.Loritta;
import com.mrpowergamerbr.loritta.commands.CommandBase;
import com.mrpowergamerbr.loritta.commands.CommandContext;

public class RollCommand extends CommandBase {
	@Override
	public String getLabel() {
		return "rolar";
	}

	public String getDescription() {
		return "Rola um dado e fala o resultado dele, perfeito quando voc� quer jogar um Monopoly maroto mas perdeu os dados.";
	}

	public String getUsage() {
		return "[n�mero]";
	}

	public List<String> getExample() {
		return Arrays.asList("12");
	}

	public Map<String, String> getDetailedUsage() {
		return ImmutableMap.<String, String>builder()
				.put("n�mero", "*(Opcional)* Quantos lados o dado que eu irei rolar ir� ter, padr�o: 6")
				.build();
	}

	@Override
	public void run(CommandContext context) {
		long val = 6;
		if (context.getArgs().length == 1) {
			try {
				val = Long.parseLong(context.getArgs()[0]);
				Loritta.getRandom().nextLong(1, val + 1);
			} catch (Exception e) {
				context.sendMessage(context.getAsMention(true) + "N�mero `" + context.getArgs()[0] + "` � algo irreconhec�vel para um bot como eu, sorry. :(");
				return;
			}
		}

		if (0 >= val) {
			context.sendMessage(context.getAsMention(true) + "N�mero inv�lido!");
			return;
		}

		context.sendMessage(context.getAsMention(true) + "**Resultado:** " + Loritta.getRandom().nextLong(1, val + 1));
	}
}

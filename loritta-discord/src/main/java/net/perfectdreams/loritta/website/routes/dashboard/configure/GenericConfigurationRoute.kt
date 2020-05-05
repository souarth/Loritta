package net.perfectdreams.loritta.website.routes.dashboard.configure

import com.mrpowergamerbr.loritta.Loritta
import com.mrpowergamerbr.loritta.network.Databases
import com.mrpowergamerbr.loritta.utils.locale.BaseLocale
import com.mrpowergamerbr.loritta.website.evaluate
import io.ktor.application.ApplicationCall
import net.dv8tion.jda.api.entities.Guild
import net.perfectdreams.loritta.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.website.routes.dashboard.RequiresGuildAuthLocalizedRoute
import net.perfectdreams.loritta.website.session.LorittaJsonWebSession
import net.perfectdreams.loritta.website.utils.extensions.legacyVariables
import net.perfectdreams.loritta.website.utils.extensions.respondHtml
import net.perfectdreams.temmiediscordauth.TemmieDiscordAuth
import org.jetbrains.exposed.sql.transactions.transaction

open class GenericConfigurationRoute(loritta: LorittaDiscord, path: String, val type: String, val file: String) : RequiresGuildAuthLocalizedRoute(loritta, path) {
	override suspend fun onGuildAuthenticatedRequest(call: ApplicationCall, locale: BaseLocale, discordAuth: TemmieDiscordAuth, userIdentification: LorittaJsonWebSession.UserIdentification, guild: Guild) {
		loritta as Loritta
		val variables = call.legacyVariables(locale)
		variables["saveType"] = type

		if (type == "miscellaneous") {
			val serverConfig = loritta.getOrCreateServerConfig(guild.idLong)

			val miscellaneousConfig = transaction(Databases.loritta) {
				serverConfig.miscellaneousConfig
			}

			variables["serverConfig"] = ConfigureMiscellaneousRoute.FakeServerConfig(
					ConfigureMiscellaneousRoute.FakeServerConfig.FakeMiscellaneousConfig(
							miscellaneousConfig?.enableBomDiaECia ?: false,
							miscellaneousConfig?.enableQuirky ?: false
					)
			)
		}

		call.respondHtml(evaluate(file, variables))
	}
}
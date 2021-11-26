package net.perfectdreams.loritta.cinnamon.platform.commands.videos.declarations

import net.perfectdreams.loritta.cinnamon.platform.commands.videos.CarlyAaahExecutor
import net.perfectdreams.loritta.cinnamon.platform.commands.CommandCategory
import net.perfectdreams.loritta.cinnamon.platform.commands.declarations.CommandDeclaration
import net.perfectdreams.loritta.cinnamon.i18n.I18nKeysData

object CarlyAaahCommand : CommandDeclaration {
    val I18N_PREFIX = I18nKeysData.Commands.Command.Carlyaaah

    override fun declaration() = command(listOf("carlyaaah"), CommandCategory.VIDEOS, I18N_PREFIX.Description) {
        executor = CarlyAaahExecutor
    }
}
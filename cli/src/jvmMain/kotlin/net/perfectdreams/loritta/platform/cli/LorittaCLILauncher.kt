package net.perfectdreams.loritta.platform.cli

suspend fun main(args: Array<String>) {
    val loritta = LorittaCLI()
    loritta.start()
    loritta.runArgs(args)
}
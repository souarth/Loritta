package net.perfectdreams.loritta.cinnamon.platform.commands.economy

import net.perfectdreams.discordinteraktions.common.utils.field
import net.perfectdreams.loritta.cinnamon.common.emotes.Emotes
import net.perfectdreams.loritta.cinnamon.common.utils.LorittaBovespaBrokerUtils
import net.perfectdreams.loritta.cinnamon.platform.commands.ApplicationCommandContext
import net.perfectdreams.loritta.cinnamon.platform.commands.CommandArguments
import net.perfectdreams.loritta.cinnamon.platform.commands.CommandExecutor
import net.perfectdreams.loritta.cinnamon.platform.commands.declarations.CommandExecutorDeclaration
import net.perfectdreams.loritta.cinnamon.platform.commands.economy.BrokerExecutorUtils.brokerBaseEmbed
import net.perfectdreams.loritta.cinnamon.platform.commands.economy.declarations.BrokerCommand

class BrokerPortfolioExecutor : CommandExecutor() {
    companion object : CommandExecutorDeclaration(BrokerPortfolioExecutor::class)

    override suspend fun execute(context: ApplicationCommandContext, args: CommandArguments) {
        context.deferChannelMessage()

        val stockInformations = context.loritta.services.bovespaBroker.getAllTickers()
        val userStockAssets = context.loritta.services.bovespaBroker.getUserBoughtStocks(context.user.id.value.toLong())

        if (userStockAssets.isEmpty())
            context.fail(
                context.i18nContext.get(BrokerCommand.I18N_PREFIX.Portfolio.YouDontHaveAnyShares),
                Emotes.LoriSob
            )

        context.sendMessage {
            brokerBaseEmbed(context) {
                title = "${Emotes.LoriStonks} ${context.i18nContext.get(BrokerCommand.I18N_PREFIX.Portfolio.Title)}"

                for (stockAsset in userStockAssets) {
                    val (tickerId, stockCount, stockSum, stockAverage) = stockAsset
                    val tickerName = LorittaBovespaBrokerUtils.trackedTickerCodes[stockAsset.ticker]
                    val tickerInformation = stockInformations.first { it.ticker == stockAsset.ticker }
                    val currentPrice = LorittaBovespaBrokerUtils.convertReaisToSonhos(tickerInformation.value)
                    val buyingPrice = LorittaBovespaBrokerUtils.convertToBuyingPrice(currentPrice) // Buying price
                    val sellingPrice = LorittaBovespaBrokerUtils.convertToSellingPrice(currentPrice) // Selling price
                    val emojiStatus = BrokerExecutorUtils.getEmojiStatusForTicker(tickerInformation)

                    val totalGainsIfSoldNow = LorittaBovespaBrokerUtils.convertToSellingPrice(
                        LorittaBovespaBrokerUtils.convertReaisToSonhos(
                            tickerInformation.value
                        )
                    ) * stockCount

                    val diff = totalGainsIfSoldNow - stockSum
                    val emojiProfit = when {
                        diff > 0 -> "\uD83D\uDD3C"
                        0 > diff -> "\uD83D\uDD3D"
                        else -> "⏹️"
                    }

                    val changePercentage = tickerInformation.dailyPriceVariation

                    // https://percentage-change-calculator.com/
                    val profitPercentage = ((totalGainsIfSoldNow - stockSum.toDouble()) / stockSum)

                    val youHaveSharesInThisTickerMessage = context.i18nContext.get(
                        BrokerCommand.I18N_PREFIX.Portfolio.YouHaveSharesInThisTicker(
                            stockCount,
                            stockSum,
                            totalGainsIfSoldNow,
                            diff.let { if (it > 0) "+$it" else it.toString() },
                            profitPercentage
                        )
                    )

                    if (tickerInformation.status != LorittaBovespaBrokerUtils.MARKET) {
                        field(
                            "$emojiStatus$emojiProfit `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                            """${context.i18nContext.get(BrokerCommand.I18N_PREFIX.Info.Embed.PriceBeforeMarketClose(currentPrice))}
                                |$youHaveSharesInThisTickerMessage
                            """.trimMargin(),
                            true
                        )
                    } else {
                        field(
                            "$emojiStatus$emojiProfit `${tickerId}` ($tickerName) | ${"%.2f".format(changePercentage)}%",
                            """${context.i18nContext.get(BrokerCommand.I18N_PREFIX.Info.Embed.BuyPrice(buyingPrice))}
                                |${context.i18nContext.get(BrokerCommand.I18N_PREFIX.Info.Embed.SellPrice(sellingPrice))}
                                |$youHaveSharesInThisTickerMessage""".trimMargin(),
                            true
                        )
                    }
                }
            }
        }
    }
}
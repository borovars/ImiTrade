package ImiTrade.market.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the market-data price-refresh scheduler, bound from
 * {@code app.market.scheduler.*} in application.yaml.
 *
 * <pre>
 * app:
 *   market:
 *     scheduler:
 *       enabled: true
 *       fixed-rate: 60000
 * </pre>
 *
 * @param enabled   whether the scheduler is active; when {@code false} the scheduler
 *                  bean is not created at all and no refresh job runs
 * @param fixedRate the refresh period in milliseconds
 */
@ConfigurationProperties(prefix = "app.market.scheduler")
public record SchedulerProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("60000") long fixedRate
) {
}

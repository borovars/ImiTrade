package ImiTrade.stocks.domain;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Derives the public URL of a company logo from its ticker.
 *
 * <p>Logos live as SVG files in the bundled static resources at
 * {@code classpath:static/logos/}, named after the ticker (e.g. {@code SBER.svg}).
 * Spring Boot serves them automatically at {@code /logos/{ticker}.svg}.
 *
 * <p>The available file names are scanned <b>once</b> at startup and cached, so
 * resolving a URL never touches the filesystem at request time (no per-request I/O
 * and no calls to external services). When a logo is missing for a ticker, the
 * shared {@code /logos/default.svg} placeholder is returned.
 */
@Slf4j
@Component
public class StockLogoResolver {

    /** Classpath location of the bundled SVG logos. */
    private static final String LOGOS_LOCATION = "classpath*:static/logos/*.svg";

    /** Public URL prefix under which Spring Boot serves the static {@code logos/} dir. */
    private static final String LOGOS_URL_PREFIX = "/logos/";

    /** Fallback file used when no logo exists for a ticker. */
    private static final String DEFAULT_FILE = "default";

    private final ResourcePatternResolver resourceResolver;

    /** Tickers (upper-cased, {@code .svg} stripped) discovered under {@link #LOGOS_LOCATION}. */
    private Set<String> availableLogos = Set.of();

    public StockLogoResolver() {
        this(new PathMatchingResourcePatternResolver());
    }

    /** Test-friendly constructor. */
    StockLogoResolver(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @PostConstruct
    void scanLogos() {
        try {
            availableLogos = java.util.Arrays.stream(resourceResolver.getResources(LOGOS_LOCATION))
                    .map(r -> {
                        String name = r.getFilename();
                        if (name == null) {
                            return "";
                        }
                        int dot = name.lastIndexOf('.');
                        return (dot > 0 ? name.substring(0, dot) : name).toUpperCase();
                    })
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException ex) {
            log.warn("Failed to scan bundled logos at {}: returning default for every ticker", LOGOS_LOCATION, ex);
            availableLogos = Set.of();
        }
        if (availableLogos.isEmpty()) {
            log.warn("No bundled logos found under {}; every ticker will resolve to default.svg", LOGOS_LOCATION);
        } else if (!availableLogos.contains(DEFAULT_FILE.toUpperCase())) {
            log.warn("default.svg is missing from bundled logos; fallback for unknown tickers will 404");
        } else {
            log.info("Loaded {} bundled company logos", availableLogos.size());
        }
    }

    /**
     * Resolves the public logo URL for the given ticker.
     *
     * <p>Returns {@code /logos/{TICKER}.svg} when a logo exists (case-insensitive
     * ticker match), otherwise {@code /logos/default.svg}. A blank ticker always
     * resolves to the default.
     */
    public String resolve(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return LOGOS_URL_PREFIX + DEFAULT_FILE + ".svg";
        }
        String key = ticker.trim().toUpperCase();
        String file = availableLogos.contains(key) ? key : DEFAULT_FILE;
        return LOGOS_URL_PREFIX + file + ".svg";
    }
}

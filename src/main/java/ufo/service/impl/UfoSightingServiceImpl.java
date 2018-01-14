package ufo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufo.dto.UfoSighting;
import ufo.service.UfoSightingService;
import ufo.util.DateUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UfoSightingServiceImpl implements UfoSightingService {
    private static final Logger LOG = LoggerFactory.getLogger(UfoSightingServiceImpl.class.getName());
    private static final Function<String, String> NULLSAFEGET = s -> s == null ? "" : s;

    private final Path ufoFilePath;
    private Queue<String> sightingErrorRepository;

    public UfoSightingServiceImpl() {
        this.ufoFilePath = Paths.get(UfoSightingServiceImpl.class.getResource("/ufo_awesome.tsv").getPath());
        this.sightingErrorRepository = new LinkedList<>();
    }

    public UfoSightingServiceImpl(String filePathURI) {
        this.ufoFilePath = Paths.get(filePathURI);
        this.sightingErrorRepository = new LinkedList<>();
    }

    @Override
    public List<UfoSighting> getAllSightings() {
        return readUfoSightings();
    }

    @Override
    public List<UfoSighting> search(int yearSeen, int monthSeen) {
        boolean isValidYearMonth = DateUtil.isValidYearMonth(yearSeen, monthSeen);
        if (!isValidYearMonth) throw new IllegalArgumentException("Year/Month parameters for search are not valid");

        List<UfoSighting> ufoSightingOnPeriod = getSightingsByDuration(
                ufoSighting -> ufoSighting != null,
                ufoSight -> NULLSAFEGET.apply(ufoSight.getDateSeen()).contains(String.join("", String.valueOf(yearSeen), String.valueOf((monthSeen)))));

        return ufoSightingOnPeriod == null ? Collections.emptyList() : ufoSightingOnPeriod;
    }

    private List<UfoSighting> readUfoSightings() {
        List<UfoSighting> ufoSightingList = null;

        try (Stream<String> lines = Files.lines(ufoFilePath)) {
            ufoSightingList = lines
                    .map(line -> {
                        String[] lineToken = line.split("\t");

                        if (lineToken.length != 6) {
                            sightingErrorRepository.offer(Stream.of(lineToken).collect(Collectors.joining(",")));
                            LOG.info("The UFO entry in file has {} entries (hence skipped)", lineToken.length);
                            return null;
                        }

                        return parseTokens(lineToken);
                    })
                    .collect(Collectors.toList());
        } catch (IOException ioe) {
            LOG.error("Error whilst reading feed file");
            throw new RuntimeException("Error whilst reading feed file", ioe);
        }

        return ufoSightingList == null ? Collections.emptyList() : ufoSightingList;
    }

    private List<UfoSighting> getSightingsByDuration(Predicate<UfoSighting> notNullable, Predicate<UfoSighting> yearMonthSight) {
        List<UfoSighting> ufoSightingBySearchCriteria = null;

        try (Stream<String> lines = Files.lines(ufoFilePath)) {
            ufoSightingBySearchCriteria = lines
                    .map(line -> {
                        String[] lineToken = line.split("\t");

                        if (lineToken.length != 6) {
                            sightingErrorRepository.offer(Stream.of(lineToken).collect(Collectors.joining(",")));
                            LOG.info("The UFO entry in file has {} entries (hence skipped)", lineToken.length);
                            return null;
                        }

                        return parseTokens(lineToken);
                    })
                    .filter(notNullable)
                    .filter(yearMonthSight)
                    .collect(Collectors.toList());
        } catch (IOException ioe) {
            LOG.error("Error whilst reading feed file");
            throw new RuntimeException("Error whilst reading feed file", ioe);
        }

        return ufoSightingBySearchCriteria;
    }

    private UfoSighting parseTokens(String[] lineToken) {
        return new UfoSighting(lineToken[0].trim(), lineToken[1].trim(), lineToken[2].trim(), lineToken[3].trim(), lineToken[4].trim(), lineToken[5].trim());
    }
}

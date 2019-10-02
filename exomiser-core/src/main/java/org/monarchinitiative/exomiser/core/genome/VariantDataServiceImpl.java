/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2019 Queen Mary University of London.
 * Copyright (c) 2012-2016 Charité Universitätsmedizin Berlin and Genome Research Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.monarchinitiative.exomiser.core.genome;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.exomiser.core.genome.dao.*;
import org.monarchinitiative.exomiser.core.model.Variant;
import org.monarchinitiative.exomiser.core.model.frequency.Frequency;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityScore;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * Default implementation of the VariantDataService.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class VariantDataServiceImpl implements VariantDataService {

    private static final Logger logger = LoggerFactory.getLogger(VariantDataServiceImpl.class);

    private final VariantWhiteList whiteList;
    // Default data sources
    private final FrequencyDao defaultFrequencyDao;
    private final PathogenicityDao defaultPathogenicityDao;

    // Optional data sources
    private final FrequencyDao localFrequencyDao;
    private final PathogenicityDao caddDao;
    private final PathogenicityDao remmDao;
    private final PathogenicityDao testPathScoreDao;

    // Structural variant data sources
    private final FrequencyDao svFrequencyDao;
    private final PathogenicityDao svPathogenicityDao;

    private VariantDataServiceImpl(Builder builder) {

        this.whiteList = builder.variantWhiteList;

        this.defaultFrequencyDao = builder.defaultFrequencyDao;
        this.defaultPathogenicityDao = builder.defaultPathogenicityDao;

        this.localFrequencyDao = builder.localFrequencyDao;
        this.caddDao = builder.caddDao;
        this.remmDao = builder.remmDao;
        this.testPathScoreDao = builder.testPathScoreDao;

        var testSv = false;
        if (testSv) {
            DataSource svDataSource = svDataSource();
            this.svFrequencyDao = new SvFrequencyDao(svDataSource);
            this.svPathogenicityDao = new SvPathogenicityDao(svDataSource);
        } else {
            this.svFrequencyDao = builder.svFrequencyDao;
            this.svPathogenicityDao = builder.svPathogenicityDao;
        }
    }

    // temporary hack
    private HikariDataSource svDataSource() {
        Path dbPath = Path.of("/Users/hhx640/Documents/sv_build/hg19_sv_database");
//        Path dbPath = Path.of("/Users/damiansmedley/exomiser-data/hg19_sv_database");

        String startUpArgs = ";SCHEMA=PBGA;DATABASE_TO_UPPER=FALSE;IFEXISTS=TRUE;AUTO_RECONNECT=TRUE;ACCESS_MODE_DATA=r;";

        String jdbcUrl = String.format("jdbc:h2:file:%s%s", dbPath.toAbsolutePath(), startUpArgs);

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(3);
        config.setPoolName("exomiser-sv");
        logger.debug("Set up {} pool {} connections from {}", config.getPoolName(), config.getMaximumPoolSize(), config.getJdbcUrl());
        return new HikariDataSource(config);
    }

    @Override
    public boolean variantIsWhiteListed(Variant variant) {
        return whiteList.contains(variant);
    }

    @Override
    public FrequencyData getVariantFrequencyData(Variant variant, Set<FrequencySource> frequencySources) {

        if (variant.isStructuralVariant()) {
            return svFrequencyDao.getFrequencyData(variant);
        }
        // This could be run alongside the pathogenicities as they are all stored in the same datastore
        FrequencyData defaultFrequencyData = defaultFrequencyDao.getFrequencyData(variant);

        List<Frequency> allFrequencies = new ArrayList<>();
        for (Frequency frequency : defaultFrequencyData.getKnownFrequencies()) {
            if (frequencySources.contains(frequency.getSource())) {
                allFrequencies.add(frequency);
            }
        }

        if (frequencySources.contains(FrequencySource.LOCAL)) {
            FrequencyData localFrequencyData = localFrequencyDao.getFrequencyData(variant);
            allFrequencies.addAll(localFrequencyData.getKnownFrequencies());
        }

        return FrequencyData.of(defaultFrequencyData.getRsId(), allFrequencies);
    }

    @Override
    public PathogenicityData getVariantPathogenicityData(Variant variant, Set<PathogenicitySource> pathogenicitySources) {

        if (variant.isStructuralVariant()) {
            return svPathogenicityDao.getPathogenicityData(variant);
        }

        // This could be run alongside the frequencies as they are all stored in the same datastore
        PathogenicityData defaultPathogenicityData = defaultPathogenicityDao.getPathogenicityData(variant);
        if (pathogenicitySources.isEmpty()) {
            // Fast-path for the unlikely case when no sources are defined - we'll just return the ClinVar data
            return PathogenicityData.of(defaultPathogenicityData.getClinVarData());
        }

        List<PathogenicityScore> allPathScores = new ArrayList<>();
        // we're going to deliberately ignore synonymous variants from dbNSFP as these shouldn't be there
        // e.g. ?assembly=hg37&chr=1&start=158581087&ref=G&alt=A has a MutationTaster score of 1
        if (variant.getVariantEffect() != VariantEffect.SYNONYMOUS_VARIANT) {
            addAllWantedScores(pathogenicitySources, defaultPathogenicityData, allPathScores);
        }

        List<PathogenicityData> optionalPathData = getOptionalPathogenicityData(variant, pathogenicitySources);
        for (PathogenicityData pathogenicityData : optionalPathData) {
            allPathScores.addAll(pathogenicityData.getPredictedPathogenicityScores());
        }

        return PathogenicityData.of(defaultPathogenicityData.getClinVarData(), allPathScores);
    }

    private void addAllWantedScores(Set<PathogenicitySource> pathogenicitySources, PathogenicityData defaultPathogenicityData, List<PathogenicityScore> allPathScores) {
        for (PathogenicityScore score : defaultPathogenicityData.getPredictedPathogenicityScores()) {
            if (pathogenicitySources.contains(score.getSource())) {
                allPathScores.add(score);
            }
        }
    }

    private List<PathogenicityData> getOptionalPathogenicityData(Variant variant, Set<PathogenicitySource> pathogenicitySources) {
        List<PathogenicityDao> daosToQuery = new ArrayList<>();
        // REMM is trained on non-coding regulatory bits of the genome, this outperforms CADD for non-coding variants
        if (pathogenicitySources.contains(PathogenicitySource.REMM) && variant.isNonCodingVariant()) {
            daosToQuery.add(remmDao);
        }

        // CADD does all of it although is not as good as REMM for the non-coding regions.
        if (pathogenicitySources.contains(PathogenicitySource.CADD)) {
            daosToQuery.add(caddDao);
        }

        if (pathogenicitySources.contains(PathogenicitySource.TEST)) {
            daosToQuery.add(testPathScoreDao);
        }

        return daosToQuery.parallelStream()
                .map(pathDao -> pathDao.getPathogenicityData(variant))
                .collect(toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private VariantWhiteList variantWhiteList = InMemoryVariantWhiteList.empty();

        private FrequencyDao defaultFrequencyDao;
        private PathogenicityDao defaultPathogenicityDao;

        private FrequencyDao localFrequencyDao;

        private PathogenicityDao caddDao;
        private PathogenicityDao remmDao;
        private PathogenicityDao testPathScoreDao;

        private FrequencyDao svFrequencyDao = new StubFrequencyDao();
        private PathogenicityDao svPathogenicityDao = new StubPathogenicityDao();

        public Builder variantWhiteList(VariantWhiteList variantWhiteList) {
            this.variantWhiteList = variantWhiteList;
            return this;
        }

        public Builder defaultFrequencyDao(FrequencyDao defaultFrequencyDao) {
            this.defaultFrequencyDao = defaultFrequencyDao;
            return this;
        }

        public Builder defaultPathogenicityDao(PathogenicityDao defaultPathogenicityDao) {
            this.defaultPathogenicityDao = defaultPathogenicityDao;
            return this;
        }

        public Builder localFrequencyDao(FrequencyDao localFrequencyDao) {
            this.localFrequencyDao = localFrequencyDao;
            return this;
        }

        public Builder caddDao(PathogenicityDao caddDao) {
            this.caddDao = caddDao;
            return this;
        }

        public Builder remmDao(PathogenicityDao remmDao) {
            this.remmDao = remmDao;
            return this;
        }

        public Builder testPathScoreDao(PathogenicityDao testPathScoreDao) {
            this.testPathScoreDao = testPathScoreDao;
            return this;
        }

        public Builder svFrequencyDao(FrequencyDao svFrequencyDao) {
            this.svFrequencyDao = svFrequencyDao;
            return this;
        }

        public Builder svPathogenicityDao(PathogenicityDao svPathogenicityDao) {
            this.svPathogenicityDao = svPathogenicityDao;
            return this;
        }

        public VariantDataServiceImpl build() {
            return new VariantDataServiceImpl(this);
        }
    }

    private static class StubFrequencyDao implements FrequencyDao {
        @Override
        public FrequencyData getFrequencyData(Variant variant) {
            return FrequencyData.empty();
        }
    }

    private static class StubPathogenicityDao implements PathogenicityDao {
        @Override
        public PathogenicityData getPathogenicityData(Variant variant) {
            return PathogenicityData.empty();
        }
    }
}

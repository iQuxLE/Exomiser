/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2020 Queen Mary University of London.
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

package org.monarchinitiative.exomiser.cli;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Timestamp;
import de.charite.compbio.jannovar.mendel.SubModeOfInheritance;
import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.api.v1.AnalysisProto;
import org.monarchinitiative.exomiser.api.v1.JobProto;
import org.monarchinitiative.exomiser.api.v1.OutputProto;
import org.monarchinitiative.exomiser.api.v1.SampleProto;
import org.monarchinitiative.exomiser.core.analysis.AnalysisMode;
import org.monarchinitiative.exomiser.core.analysis.AnalysisProtoBuilder;
import org.monarchinitiative.exomiser.core.analysis.util.InheritanceModeOptions;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;

import java.time.Instant;
import java.util.List;

import static de.charite.compbio.jannovar.annotation.VariantEffect.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.monarchinitiative.exomiser.api.v1.OutputProto.OutputFormat.*;
import static org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource.*;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class CommandLineJobReaderTest {

    private final CommandLineJobReader instance = new CommandLineJobReader();

    private static final SampleProto.Sample SAMPLE = SampleProto.Sample.newBuilder()
            .setGenomeAssembly("hg19")
            .setVcf("examples/Pfeiffer.vcf")
            .setPed("examples/Pfeiffer-singleton.ped")
            .setProband("manuel")
            .addAllHpoIds(List.of("HP:0001156", "HP:0001363", "HP:0011304", "HP:0010055"))
            .build();

    private static final Phenopacket PHENOPACKET = Phenopacket.newBuilder()
            .setId("manuel")
            .setSubject(Individual.newBuilder()
                    .setId("manuel")
                    .setSex(Sex.MALE)
                    .build())
            .addPhenotypicFeatures(PhenotypicFeature.newBuilder().setType(OntologyClass.newBuilder()
                    .setId("HP:0001159")
                    .setLabel("Syndactyly")
                    .build()))
            .addPhenotypicFeatures(PhenotypicFeature.newBuilder().setType(OntologyClass.newBuilder()
                    .setId("HP:0000486")
                    .setLabel("Strabismus")
                    .build()))
            .addPhenotypicFeatures(PhenotypicFeature.newBuilder().setType(OntologyClass.newBuilder()
                    .setId("HP:0000327")
                    .setLabel("Hypoplasia of the maxilla")
                    .build()))
            .addPhenotypicFeatures(PhenotypicFeature.newBuilder().setType(OntologyClass.newBuilder()
                    .setId("HP:0000520")
                    .setLabel("Proptosis")
                    .build()))
            .addPhenotypicFeatures(PhenotypicFeature.newBuilder().setType(OntologyClass.newBuilder()
                    .setId("HP:0000316")
                    .setLabel("Hypertelorism")
                    .build()))
            .addPhenotypicFeatures(PhenotypicFeature.newBuilder().setType(OntologyClass.newBuilder()
                    .setId("HP:0000244")
                    .setLabel("Brachyturricephaly")
                    .build()))
            .addHtsFiles(HtsFile.newBuilder()
                    .setUri("file:///data/Pfeiffer.vcf")
                    .setHtsFormat(HtsFile.HtsFormat.VCF)
                    .setGenomeAssembly("hg19")
                    .build())
            .setMetaData(MetaData.newBuilder()
                    .setCreated(Timestamp.newBuilder()
                            .setSeconds(Instant.parse("2019-11-12T13:47:51.948Z").getEpochSecond())
                            .setNanos(Instant.parse("2019-11-12T13:47:51.948Z").getNano())
                            .build())
                    .setCreatedBy("julesj")
                    .addResources(Resource.newBuilder()
                            .setId("hp")
                            .setName("human phenotype ontology")
                            .setUrl("http://purl.obolibrary.org/obo/hp.owl")
                            .setVersion("hp/releases/2019-11-08")
                            .setNamespacePrefix("HP")
                            .setIriPrefix("http://purl.obolibrary.org/obo/HP_")
                            .build())
                    .setPhenopacketSchemaVersion("1.0.0")
                    .build())
            .build();

    private static final OutputProto.OutputOptions OUTPUT = OutputProto.OutputOptions.newBuilder()
            .setOutputPrefix("results/Pfeiffer-hiphive-exome")
            .setOutputContributingVariantsOnly(false)
            .setNumGenes(0)
            .addAllOutputFormats(List.of(HTML, JSON, TSV_GENE, TSV_VARIANT, VCF))
            .build();

    private static final OutputProto.OutputOptions DEFAULT_OUTPUT_OPTIONS = OutputProto.OutputOptions.newBuilder()
            .setOutputPrefix("")
            .setOutputContributingVariantsOnly(false)
            .setNumGenes(0)
            .addAllOutputFormats(List.of(HTML, JSON))
            .build();

    private static final AnalysisProto.Analysis ANALYSIS = AnalysisProtoBuilder.builder()
            .analysisMode(AnalysisMode.PASS_ONLY)
            .inheritanceModes(InheritanceModeOptions.of(
                    new ImmutableMap.Builder<SubModeOfInheritance, Float>()
                            .put(SubModeOfInheritance.AUTOSOMAL_DOMINANT, 0.1f)
                            .put(SubModeOfInheritance.AUTOSOMAL_RECESSIVE_COMP_HET, 2.0f)
                            .put(SubModeOfInheritance.AUTOSOMAL_RECESSIVE_HOM_ALT, 0.1f)
                            .put(SubModeOfInheritance.X_DOMINANT, 0.1f)
                            .put(SubModeOfInheritance.X_RECESSIVE_COMP_HET, 2.0f)
                            .put(SubModeOfInheritance.X_RECESSIVE_HOM_ALT, 0.1f)
                            .put(SubModeOfInheritance.MITOCHONDRIAL, 0.2f)
                            .build()
            ))
            .frequencySources(ImmutableSet.of(
                    FrequencySource.THOUSAND_GENOMES,
                    FrequencySource.TOPMED,
                    FrequencySource.UK10K,

                    FrequencySource.ESP_AFRICAN_AMERICAN,
                    FrequencySource.ESP_EUROPEAN_AMERICAN,
                    FrequencySource.ESP_ALL,

                    FrequencySource.EXAC_AFRICAN_INC_AFRICAN_AMERICAN,
                    FrequencySource.EXAC_AMERICAN,
                    FrequencySource.EXAC_SOUTH_ASIAN,
                    FrequencySource.EXAC_EAST_ASIAN,
                    FrequencySource.EXAC_FINNISH,
                    FrequencySource.EXAC_NON_FINNISH_EUROPEAN,
                    FrequencySource.EXAC_OTHER,

                    FrequencySource.GNOMAD_E_AFR,
                    FrequencySource.GNOMAD_E_AMR,
                    FrequencySource.GNOMAD_E_EAS,
                    FrequencySource.GNOMAD_E_FIN,
                    FrequencySource.GNOMAD_E_NFE,
                    FrequencySource.GNOMAD_E_OTH,
                    FrequencySource.GNOMAD_E_SAS,

                    FrequencySource.GNOMAD_G_AFR,
                    FrequencySource.GNOMAD_G_AMR,
                    FrequencySource.GNOMAD_G_EAS,
                    FrequencySource.GNOMAD_G_FIN,
                    FrequencySource.GNOMAD_G_NFE,
                    FrequencySource.GNOMAD_G_OTH,
                    FrequencySource.GNOMAD_G_SAS
            ))
            .pathogenicitySources(ImmutableSet.of(POLYPHEN, MUTATION_TASTER, SIFT))
            .addVariantEffectFilter(ImmutableSet.of(
                    FIVE_PRIME_UTR_EXON_VARIANT,
                    FIVE_PRIME_UTR_INTRON_VARIANT,
                    THREE_PRIME_UTR_EXON_VARIANT,
                    THREE_PRIME_UTR_INTRON_VARIANT,
                    NON_CODING_TRANSCRIPT_EXON_VARIANT,
                    UPSTREAM_GENE_VARIANT,
                    INTERGENIC_VARIANT,
                    REGULATORY_REGION_VARIANT,
                    CODING_TRANSCRIPT_INTRON_VARIANT,
                    NON_CODING_TRANSCRIPT_INTRON_VARIANT,
                    DOWNSTREAM_GENE_VARIANT))
            .addFrequencyFilter(2.0f)
            .addPathogenicityFilter(true)
            .addInheritanceFilter()
            .addOmimPrioritiser()
            .addHiPhivePrioritiser()
            .build();

    private static final JobProto.Job PFEIFFER_SAMPLE_JOB = JobProto.Job.newBuilder()
            .setSample(SAMPLE)
            .setAnalysis(ANALYSIS)
            .setOutputOptions(OUTPUT)
            .build();

    private static final JobProto.Job PFEIFFER_PHENOPACKET_JOB = JobProto.Job.newBuilder()
            .setPhenopacket(PHENOPACKET)
            .setAnalysis(ANALYSIS)
            .setOutputOptions(OUTPUT)
            .build();

    @Test
    void readIllegalAnalysisOutputNoSampleCombination() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--analysis", "src/test/resources/exome-analysis.yml",
                "--output", "src/test/resources/pfeiffer_output_options.yml"
        );
        assertThrows(CommandLineParseError.class, () -> instance.readJobs(commandLine));
    }

    @Test
    void readIllegalAnalysisBatchOutputNoSampleCombination() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--analysis-batch", "src/test/resources/batch-analysis-job.txt",
                "--output", "src/test/resources/pfeiffer_output_options.yml"
        );
        assertThrows(CommandLineParseError.class, () -> instance.readJobs(commandLine));
    }

    @Test
    void readCliAnalysisLegacy() {
        CommandLine commandLine = CommandLineOptionsParser.parse("--analysis", "src/test/resources/pfeiffer_analysis_v8_12.yml");
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_SAMPLE_JOB)));
    }

    @Test
    void readCliAnalysisWithJobSample() {
        CommandLine commandLine = CommandLineOptionsParser.parse("--analysis", "src/test/resources/pfeiffer_job_sample.yml");
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_SAMPLE_JOB)));
    }

    @Test
    void readAnalysisBatchWithJobs() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--analysis-batch", "src/test/resources/batch-analysis-job.txt"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_SAMPLE_JOB, PFEIFFER_PHENOPACKET_JOB)));
    }

    @Test
    void readAnalysisBatchWithLegacyAnalysis() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--analysis-batch", "src/test/resources/batch-analysis-job.txt"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_SAMPLE_JOB, PFEIFFER_PHENOPACKET_JOB)));
    }

    @Test
    void readAnalysisBatchWithMixedLegacyAndJob() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--analysis-batch", "src/test/resources/batch-analysis-mixed-job-legacy.txt"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_SAMPLE_JOB, PFEIFFER_PHENOPACKET_JOB)));
    }

    @Test
    void readAnalysisBatchWithMixedLegacyAndNewAnalysis() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--analysis-batch", "src/test/resources/batch-analysis-with-new-no-sample-analysis.txt"
        );
        assertThrows(IllegalArgumentException.class, () -> instance.readJobs(commandLine));
    }

    @Test
    void readCliNewAnalysisOnlyThrowsException() {
        CommandLine commandLine = CommandLineOptionsParser.parse("--analysis", "src/test/resources/exome-analysis.yml");
        assertThrows(IllegalArgumentException.class, () -> instance.readJobs(commandLine));
    }

    @Test
    void readCliJobSample() {
        CommandLine commandLine = CommandLineOptionsParser.parse("--job", "src/test/resources/pfeiffer_job_sample.yml");
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_SAMPLE_JOB)));
    }

    @Test
    void readCliJobPhenopacket() {
        CommandLine commandLine = CommandLineOptionsParser.parse("--job", "src/test/resources/pfeiffer_job_phenopacket.yml");
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_PHENOPACKET_JOB)));
    }

    @Test
    void readCliSampleAnalysisOutput() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_sample.yml",
                "--analysis", "src/test/resources/exome-analysis.yml",
                "--output", "src/test/resources/pfeiffer_output_options.yml"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);
        assertThat(jobs, equalTo(List.of(PFEIFFER_SAMPLE_JOB)));
    }

    @Test
    void readCliSampleOnlyWithSample() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_sample.yml"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setSample(SAMPLE)
                .setPreset(AnalysisProto.Preset.EXOME)
                .setOutputOptions(DEFAULT_OUTPUT_OPTIONS)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSampleOnlyWithPhenopacket() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_phenopacket.yml"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setPhenopacket(PHENOPACKET)
                .setPreset(AnalysisProto.Preset.EXOME)
                .setOutputOptions(DEFAULT_OUTPUT_OPTIONS)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSamplePresetExome() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_sample.yml",
                "--preset=exome"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setSample(SAMPLE)
                .setPreset(AnalysisProto.Preset.EXOME)
                .setOutputOptions(DEFAULT_OUTPUT_OPTIONS)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSampleIllegalPreset() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_sample.yml",
                "--preset=wibble!"
        );
        assertThrows(IllegalArgumentException.class, () -> instance.readJobs(commandLine));
    }

    @Test
    void readCliSamplePresetGenome() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_sample.yml",
                "--preset=genome"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setSample(SAMPLE)
                .setPreset(AnalysisProto.Preset.GENOME)
                .setOutputOptions(DEFAULT_OUTPUT_OPTIONS)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSamplePresetWithPhenopacket() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_phenopacket.yml",
                "--preset=exome"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setPhenopacket(PHENOPACKET)
                .setPreset(AnalysisProto.Preset.EXOME)
                .setOutputOptions(DEFAULT_OUTPUT_OPTIONS)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSamplePresetOutputWithPhenopacket() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_phenopacket.yml",
                "--preset=exome",
                "--output", "src/test/resources/pfeiffer_output_options.yml"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setPhenopacket(PHENOPACKET)
                .setPreset(AnalysisProto.Preset.EXOME)
                .setOutputOptions(OUTPUT)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSampleOutputWithPhenopacket() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_phenopacket.yml",
                "--output", "src/test/resources/pfeiffer_output_options.yml"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setPhenopacket(PHENOPACKET)
                .setPreset(AnalysisProto.Preset.EXOME)
                .setOutputOptions(OUTPUT)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSampleAnalysisWithPhenopacket() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_phenopacket.yml",
                "--analysis", "src/test/resources/exome-analysis.yml"
        );
        List<JobProto.Job> jobs = instance.readJobs(commandLine);

        JobProto.Job expected = JobProto.Job.newBuilder()
                .setPhenopacket(PHENOPACKET)
                .setAnalysis(ANALYSIS)
                .setOutputOptions(DEFAULT_OUTPUT_OPTIONS)
                .build();

        assertThat(jobs, equalTo(List.of(expected)));
    }

    @Test
    void readCliSampleWithLegacyAnalysisThrowsException() {
        CommandLine commandLine = CommandLineOptionsParser.parse(
                "--sample", "src/test/resources/pfeiffer_phenopacket.yml",
                "--analysis", "src/test/resources/pfeiffer_analysis_v8_12.yml"
        );
        // the old analysis is really now a job, so it could be read as a Job, but then the sample would be over-writing
        // the sample details supplied in the old analysis job which can make for confusing behaviour. For this reason
        // I've chosen to make this strictly require the new analysis.yml format.
        assertThrows(IllegalArgumentException.class, () -> instance.readJobs(commandLine));
    }
}
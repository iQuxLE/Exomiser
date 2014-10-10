The Exomiser - Command Line Executable 
===============================================================
# Change log (version numbers follow semantic versioning - semver.org)  

## 4.0.0 2014-09-19
- Changed FilterScores to FilterResults to encapsulate the pass/fail , score and filterTypes returned from the filters in various ways previously.
- Changed Filter behaviour to simply return a FilterResult instead of altering the VariantEvaluation in inconsistent ways.
- VariantEvaluation now hides a bit more of its inner workings regarding FilterResults.
- PathogenicityData will now return its own most pathogenic score instead of relying on something else to figure this out.

- Major changes to PathogenicityFilter behaviour - Missense variants will always pass the filter regardless of their predicted pathogenicity. Other variant types can now be filtered according to the cutoff score or allowed to pass irrespective of the score.  
- Command line option changes:
    -P --remove-path-filter-cutoff command line option added to allow all variant types through pathogenicity filter.
    -P --keep-non-pathogenic-missense command line option removed.
    -P option default changed from true to false! Sorry. Check your existing settings carefully!

- Added GeneFilter functionality
- Renamed Scorable interface to Score
- Renamed VariantEvaluation variables and accessor methods:
    getFilterScoreMap to getFilterResults to match how it is referred to in the code output.
    getFailedFilters to getFailedFilterTypes
    passesFilters to passedFilters

- Bug-fixes 
    - Prioritisers now release database connections when finished (affects batch-mode performance)
    - Inheritance filter now performs correctly in all cases.

## 3.0.2 2014-09-08
- VCF output now contains original VCF INFO field with exomiser info appended onto this. 
- Bug-fix for crash when Jannovar found no annotations for a variant.

## 3.0.1 2014-09-04
- Bug-fix for duplicate variants in Frequency table where the RSID was different.

## 3.0.0 2014-08-22
- Completely re-worked under the hood code
- New extensible API
- Simplified command-line usage
- Multiple output formats
- Batch mode analysis
- Settings file input
- Zero-config installation 

## 2.1.0 2014-05-06
- Embedded H2 database or PostgreSQL
- Simplified set-up/installation

# Installation

Unpack the exomiser-cli-${project.version}.zip and it's ready to go.

# Alternative set-up

If you want to run Exomiser using an H2 database from a location of your choosing edit the line in application.properties:

    h2Path=

with

    h2Path=/full/path/to/alternative/h2/database/exomiser.h2.db

(optional) If you want to run from a Postgres database rather than the default H2 embedded database
  
    (a) download exomiser_dump.pg.gz
    (b) gunzip exomiser_dump.pg.gz
    (c) load into your postgres server: psql -h yourhost -U yourusername yourdatabase < exomiser_dump.pg
    (d) edit application.properties with the details of how to connect this new database

# Usage

(a) Exomiser v2 - phenotype comparisons to human, mouse and fish involving disruption of the gene or nearby genes in the interactome using a RandomWalk 

    java -Xms5g -Xmx5g -jar exomiser-cli-${project.version}.jar --prioritiser=exomiser-allspecies -I AD -F 1 -D OMIM:101600 -v data/Pfeiffer.vcf 

    java -Xms5g -Xmx5g -jar exomiser-cli-${project.version}.jar --prioritiser=exomiser-allspecies -I AD -F 1 --hpo-ids HP:0000006,HP:0000174,HP:0000194,HP:0000218,HP:0000238,HP:0000244,HP:0000272,HP:0000303,HP:0000316,HP:0000322,HP:0000324,HP:0000327,HP:0000348,HP:0000431,HP:0000452,HP:0000453,HP:0000470,HP:0000486,HP:0000494,HP:0000508,HP:0000586,HP:0000678,HP:0001156,HP:0001249,HP:0002308,HP:0002676,HP:0002780,HP:0003041,HP:0003070,HP:0003196,HP:0003272,HP:0003307,HP:0003795,HP:0004209,HP:0004322,HP:0004440,HP:0005048,HP:0005280,HP:0005347,HP:0006101,HP:0006110,HP:0009602,HP:0009773,HP:0010055,HP:0010669,HP:0011304 -v data/Pfeiffer.vcf

(b) Exomiser v1 - phenotype comparisons to mice with disruption of the gene

    java -Xmx2g -jar exomiser-cli-${project.version}.jar --prioritiser=exomiser-mouse -I AD -F 1 -M -D OMIM:101600 -v data/Pfeiffer.vcf

Phenix - phenotype comparisons to known human disease genes

    java -Xms5g -Xmx5g -jar exomiser-cli-${project.version}.jar --prioritiser=phenix -v data/Pfeiffer.vcf -I AD -F 1 --hpo-ids HP:0000006,HP:0000174,HP:0000194,HP:0000218,HP:0000238,HP:0000244,HP:0000272,HP:0000303,HP:0000316,HP:0000322,HP:0000324,HP:0000327,HP:0000348,HP:0000431,HP:0000452,HP:0000453,HP:0000470,HP:0000486,HP:0000494,HP:0000508,HP:0000586,HP:0000678,HP:0001156,HP:0001249,HP:0002308,HP:0002676,HP:0002780,HP:0003041,HP:0003070,HP:0003196,HP:0003272,HP:0003307,HP:0003795,HP:0004209,HP:0004322,HP:0004440,HP:0005048,HP:0005280,HP:0005347,HP:0006101,HP:0006110,HP:0009602,HP:0009773,HP:0010055,HP:0010669,HP:0011304

(c) ExomeWalker - prioritisation by proximity in interactome to the seed genes

    java -Xms5g -Xmx5g -jar exomiser-cli-${project.version}.jar --prioritiser exomewalker  -v data/Pfeiffer.vcf -I AD -F 1 -S 2260

# Other useful params:

Multiple output formats:

    --output-format TSV (TSV summary instead of HTML)
    --output-format VCF (VCF summary instead of HTML)
    --output-format TSV,VCF (TSV and VCF summary instead of HTML)

Settings file:
    
Settings files contain all the parameters passed in on the command-line so you can just point exomiser to a file. See example.settings and test.settings.

    java -Xms5g -Xmx5g -jar exomiser-cli-${project.version}.jar --settings-file test.settings

    
Alternatively you can mix up a settings file and override settings by specifying them on the command line:

    java -Xms5g -Xmx5g -jar exomiser-cli-${project.version}.jar --settings-file test.settings --prioritiser=phenix


Batch mode analysis:
    
Batch mode will run through a list of settings files. Simple put the path to each settings file in the batch file - one file path per line.

    java -Xms5g -Xmx5g -jar exomiser-cli-${project.version}.jar --batch-file batch.txt

    -T leave in off-target variants

Want help? 

    java -jar exomiser-cli-${project.version}.jar --help

   
# Project Build

This maven project is used to build the main exomiser jar for distribution. The 
assembly plugin will produce a zip and tar.gz with an internal structure defined 
in src/assemble/distribution.xml. This automates most of the following steps, 
although the maven resources plugin will copy all resources and data files to 
target/resources and the assembly plugin will copy from here to the directories 
defined in the outputDirectory fields for each fileSet of distribution.xml:

    cd target
    mkdir data
    # copy in the data made from the db build or run this now if you haven't already
    cp ../../exomiser-db/data/exomiser.h2.db data/.
    cp ../../exomiser-db/data/extracted/ucsc_hg19.ser data/.
    # copy in the rw_string_9_05* data to data/
    ... from somewhere
    # copy in the extra phenix data to data/
    ... from somewhere
    # make the archive.
    tar -cvzf exomiser.tgz exomiser-cli-${project.version}.jar jdbc.properties log4j2.xml lib data 
    # copy to the ftp site
    scp exomiser.tgz gen1:/nfs/disk69/ftp/pub/resources/software/exomiser/downloads/exomiser/ 
/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2018 Queen Mary University of London.
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

import org.monarchinitiative.exomiser.core.model.*;

import java.util.List;
import java.util.Set;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public interface GenomeDataService {

    public List<Gene> getKnownGenes();

    public Set<GeneIdentifier> getKnownGeneIdentifiers();

    public List<RegulatoryFeature> getRegulatoryFeatures();

    public default ChromosomalRegionIndex<RegulatoryFeature> getRegulatoryRegionIndex() {
        return ChromosomalRegionIndex.of(getRegulatoryFeatures());
    }

    public List<TopologicalDomain> getTopologicallyAssociatedDomains();

    public default ChromosomalRegionIndex<TopologicalDomain> getTopologicallyAssociatedDomainIndex() {
        return ChromosomalRegionIndex.of(getTopologicallyAssociatedDomains());
    }
}

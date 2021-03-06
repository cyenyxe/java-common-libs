package org.opencb.commons.bioformats.variant.vcf4.filters;

import org.opencb.commons.bioformats.variant.vcf4.VcfRecord;

/**
 * Created with IntelliJ IDEA.
 * User: aleman
 * Date: 8/26/13
 * Time: 10:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class VcfSnpFilter extends VcfFilter {


    public VcfSnpFilter(int priority) {
        super(priority);
    }

    public VcfSnpFilter() {
        super();
    }

    @Override
    public boolean apply(VcfRecord vcfRecord) {
        return (!vcfRecord.getId().equalsIgnoreCase(".") && !vcfRecord.getId().equalsIgnoreCase(""));
    }

}

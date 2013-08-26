package org.opencb.commons.bioformats.commons.core.vcffilter;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.opencb.commons.bioformats.commons.core.variant.vcf4.VcfRecord;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aleman
 * Date: 8/26/13
 * Time: 10:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class VcfFilters {

    public static List<VcfRecord> filter(List<VcfRecord> list_records, List<Predicate<VcfRecord>> filters){
        List<VcfRecord> filtered_list = null;

        Predicate<VcfRecord> and_filters = Predicates.and(filters);

        filtered_list = Lists.newArrayList(Iterables.filter(list_records, and_filters));

        return filtered_list;

    }

    public static boolean filter(VcfRecord vcf_record, List<Predicate<VcfRecord>> filters){

           Predicate<VcfRecord> and_filters = Predicates.and(filters);

        return and_filters.apply(vcf_record);

    }
}

package org.opencb.variant.lib.io;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.opencb.variant.lib.core.formats.*;
import org.opencb.variant.lib.filters.customfilters.VcfFilter;
import org.opencb.variant.lib.filters.VcfRecordFilters;
import org.opencb.variant.lib.io.ped.readers.PedDataReader;
import org.opencb.variant.lib.io.ped.readers.PedFileDataReader;
import org.opencb.variant.lib.io.ped.writers.PedDataWriter;
import org.opencb.variant.lib.io.variant.readers.VcfDataReader;
import org.opencb.variant.lib.io.variant.readers.VcfFileDataReader;
import org.opencb.variant.lib.io.variant.writers.VcfDataWriter;
import org.opencb.variant.lib.io.variant.writers.VcfSqliteDataWriter;
import org.opencb.variant.lib.stats.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aaleman
 * Date: 9/2/13
 * Time: 6:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class VariantRunner {

    private List<VcfFilter> filters;
    private int numThreads;
    private VcfDataReader vcfReader;
    private VcfDataWriter vcfWriter;
    private PedDataReader pedReader;
    private PedDataWriter pedWriter;
    private boolean effect;
    private boolean stats;
    private boolean index;


    public VariantRunner() {
        this.filters = null;
        this.numThreads = 1;
        this.stats = true;
        this.index = true;
        this.effect = true;
    }

    public VariantRunner(String vcfFilePath, String sqliteFileName, String pedFilePath) {
        this();
        vcfReader = new VcfFileDataReader(vcfFilePath);
        vcfWriter = new VcfSqliteDataWriter(sqliteFileName);

        if (pedFilePath != null) {
            pedReader = new PedFileDataReader(pedFilePath);
        }

    }

    public VariantRunner stats() {
        this.stats = true;
        return this;
    }

    public VariantRunner effect() {
        this.effect = true;
        return this;
    }

    public VariantRunner index() {
        this.index = true;
        return this;
    }

    public VariantRunner reader(VcfDataReader reader) {
        this.vcfReader = reader;
        return this;
    }

    public VariantRunner writer(VcfDataWriter writer) {
        this.vcfWriter = writer;
        return this;
    }


    public VariantRunner(VcfDataReader vcfReader, VcfDataWriter vcfWriter) {
        this();
        this.vcfReader = vcfReader;
        this.vcfWriter = vcfWriter;
    }

    public VariantRunner filter(List<VcfFilter> filterList) {
        this.filters = filterList;
        return this;
    }


    public VariantRunner parallel(int numThreads) {
        this.numThreads = numThreads;
        return this;
    }

    public void run() throws IOException {
        int batchSize = 1000;

        Pedigree ped = null;

        VcfFilter andFilter;

        VcfGlobalStat globalStat;
        VcfSampleStat vcfSampleStat;
        StringBuilder chunkVcfRecords;

        List<VcfRecord> batch;
        List<VariantEffect> batchEffect;
        List<VcfVariantStat> statsList;
        List<VcfGlobalStat> globalStats = new ArrayList<>(100);
        List<VcfSampleStat> sampleStats = new ArrayList<>(100);
        List<VcfSampleGroupStat> sampleGroupPhen = new ArrayList<>(100);
        List<VcfSampleGroupStat> sampleGroupFam = new ArrayList<>(100);

         if(pedReader != null){
             pedReader.open();
             ped = pedReader.read();
             pedReader.close();
         }


        vcfReader.open();
        vcfWriter.open();

        vcfReader.pre();
        vcfWriter.pre();


        VcfSampleGroupStat vcfSampleGroupStatPhen = new VcfSampleGroupStat();
        VcfSampleGroupStat vcfSampleGroupStatFam;


        VcfVariantGroupStat groupStatsBatchPhen = null;
        VcfVariantGroupStat groupStatsBatchFam = null;

        batch = vcfReader.read(batchSize);

        while (!batch.isEmpty()) {

            if (filters != null) {
                batch = VcfRecordFilters.filter(batch, filters);
            }

            if (stats) {
                statsList = CalculateStats.variantStats(batch, vcfReader.getSampleNames(), ped);
                globalStat = CalculateStats.globalStats(statsList);
                globalStats.add(globalStat);

                vcfSampleStat = CalculateStats.sampleStats(batch, vcfReader.getSampleNames(), ped);
                sampleStats.add(vcfSampleStat);

                if(ped != null){
                    groupStatsBatchPhen = CalculateStats.groupStats(batch, ped, "phenotype");
                    groupStatsBatchFam = CalculateStats.groupStats(batch, ped, "family");

                    vcfSampleGroupStatPhen = CalculateStats.sampleGroupStats(batch, ped, "phenotype");
                    sampleGroupPhen.add(vcfSampleGroupStatPhen);

                    vcfSampleGroupStatFam = CalculateStats.sampleGroupStats(batch, ped, "family");
                    sampleGroupFam.add(vcfSampleGroupStatFam);

                }

                vcfWriter.writeVariantStats(statsList);
                vcfWriter.writeVariantGroupStats(groupStatsBatchPhen);
                vcfWriter.writeVariantGroupStats(groupStatsBatchFam);
            }

            if (index) {
                vcfWriter.writeVariantIndex(batch);
            }

            if (effect) {


                batchEffect = CalculateStats.variantEffects(batch);
                vcfWriter.writeVariantEffect(batchEffect);

            }

            batch = vcfReader.read(batchSize);
        }

        globalStat = new VcfGlobalStat(globalStats);
        vcfSampleStat = new VcfSampleStat(vcfReader.getSampleNames(), sampleStats);
        vcfSampleGroupStatPhen = new VcfSampleGroupStat(sampleGroupPhen);
        vcfSampleGroupStatFam = new VcfSampleGroupStat(sampleGroupFam);

        vcfWriter.writeGlobalStats(globalStat);
        vcfWriter.writeSampleStats(vcfSampleStat);

        vcfWriter.writeSampleGroupStats(vcfSampleGroupStatFam);
        vcfWriter.writeSampleGroupStats(vcfSampleGroupStatPhen);

        vcfReader.post();
        vcfWriter.post();

        vcfReader.close();
        vcfWriter.close();
    }

    public boolean isEffect() {
        return effect;
    }

    public void setEffect(boolean effect) {
        this.effect = effect;
    }
}

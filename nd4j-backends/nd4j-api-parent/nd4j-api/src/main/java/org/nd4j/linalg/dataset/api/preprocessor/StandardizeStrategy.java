package org.nd4j.linalg.dataset.api.preprocessor;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastAddOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastDivOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastMulOp;
import org.nd4j.linalg.api.ops.impl.broadcast.BroadcastSubOp;
import org.nd4j.linalg.dataset.api.preprocessor.stats.DistributionStats;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Conditions;

/**
 * {@link NormalizerStrategy} implementation that will standardize and de-standardize data arrays, based on statistics
 * of the means and standard deviations of the population
 *
 * @author Ede Meijer
 */
public class StandardizeStrategy implements NormalizerStrategy<DistributionStats> {
    /**
     * Normalize a data array
     *
     * @param array the data to normalize
     * @param stats statistics of the data population
     */
    @Override
    public void preProcess(INDArray array, DistributionStats stats) {
        if (array.rank() <= 2) {
            array.subiRowVector(stats.getMean());
            array.diviRowVector(filteredStd(stats));
        }
        // if array Rank is 3 (time series) samplesxfeaturesxtimesteps
        // if array Rank is 4 (images) samplesxchannelsxrowsxcols
        // both cases operations should be carried out in dimension 1
        else {
            Nd4j.getExecutioner().execAndReturn(new BroadcastSubOp(array, stats.getMean(), array, 1));
            Nd4j.getExecutioner().execAndReturn(new BroadcastDivOp(array, filteredStd(stats), array, 1));
        }
    }

    /**
     * Denormalize a data array
     *
     * @param array the data to denormalize
     * @param stats statistics of the data population
     */
    @Override
    public void revert(INDArray array, DistributionStats stats) {
        if (array.rank() <= 2) {
            array.muliRowVector(filteredStd(stats));
            array.addiRowVector(stats.getMean());
        } else {
            Nd4j.getExecutioner().execAndReturn(new BroadcastMulOp(array, filteredStd(stats), array, 1));
            Nd4j.getExecutioner().execAndReturn(new BroadcastAddOp(array, stats.getMean(), array, 1));
        }
    }

    private static INDArray filteredStd(DistributionStats stats) {
        /*
            To avoid division by zero when the std deviation is zero, replace zeros by one
         */
        INDArray stdCopy = stats.getStd();
        BooleanIndexing.replaceWhere(stdCopy, 1.0, Conditions.equals(0));
        return stdCopy;
    }
}

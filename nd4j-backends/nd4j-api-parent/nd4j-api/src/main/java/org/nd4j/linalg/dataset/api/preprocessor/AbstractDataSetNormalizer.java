package org.nd4j.linalg.dataset.api.preprocessor;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Setter;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.stats.NormalizerStats;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

/**
 * Abstract base class for normalizers
 * that act upon {@link DataSet} instances
 * or iterators
 *
 * @author Ede Meijer
 */
@EqualsAndHashCode(callSuper = false)
abstract class AbstractDataSetNormalizer<S extends NormalizerStats> extends AbstractNormalizer<S> implements DataNormalization {
    @Setter(AccessLevel.PROTECTED) private S featureStats;
    @Setter(AccessLevel.PROTECTED) private S labelStats;
    private boolean fitLabels = false;

    protected AbstractDataSetNormalizer(NormalizerStrategy<S> strategy) {
        super(strategy);
    }

    /**
     * Flag to specify if the labels/outputs in the dataset should be also normalized
     * default value is false
     *
     * @param fitLabels
     */
    @Override
    public void fitLabel(boolean fitLabels) {
        this.fitLabels = fitLabels;
    }


    /**
     * Whether normalization for the labels is also enabled. Most commonly used for regression, not classification.
     *
     * @return True if labels will be
     */
    @Override
    public boolean isFitLabel() {
        return this.fitLabels;
    }

    /**
     * Fit a dataset (only compute based on the statistics from this dataset)
     * @param dataSet the dataset to compute on
     */
    @Override
    public void fit(DataSet dataSet) {
        featureStats = (S) newBuilder().addFeatures(dataSet).build();
        if (isFitLabel()) {
            labelStats = (S) newBuilder().addLabels(dataSet).build();
        }
    }

    protected S getFeatureStats() {
        assertIsFit();
        return featureStats;
    }

    protected S getLabelStats() {
        assertIsFit();
        return labelStats;
    }

    @Override
    protected boolean isFit() {
        return featureStats != null;
    }

    /**
     * Fit the given model
     *
     * @param iterator for the data to iterate over
     */
    @Override
    public void fit(DataSetIterator iterator) {
        S.Builder featureNormBuilder = newBuilder();
        S.Builder labelNormBuilder = newBuilder();

        iterator.reset();
        while (iterator.hasNext()) {
            DataSet next = iterator.next();
            featureNormBuilder.addFeatures(next);
            if (fitLabels) {
                labelNormBuilder.addLabels(next);
            }
        }
        featureStats = (S) featureNormBuilder.build();
        if (fitLabels) {
            labelStats = (S) labelNormBuilder.build();
        }
        iterator.reset();
    }

    protected abstract S.Builder newBuilder();

    /**
     * Pre process a dataset
     *
     * @param toPreProcess the data set to pre process
     */
    @Override
    public void preProcess(@NonNull DataSet toPreProcess) {
        transform(toPreProcess.getFeatures());
        transformLabel(toPreProcess.getLabels());
    }

    /**
     * Transform the given dataset
     *
     * @param toPreProcess
     */
    @Override
    public void transform(DataSet toPreProcess) {
        preProcess(toPreProcess);
    }

    /**
     * Transform the given INDArray
     *
     * @param theFeatures
     */
    @Override
    public void transform(INDArray theFeatures) {
        strategy.preProcess(theFeatures, getFeatureStats());
    }

    /**
     * Transform the labels. If {@link #isFitLabel()} == false, this is a no-op
     */
    @Override
    public void transformLabel(INDArray label) {
        if (isFitLabel()) {
            strategy.preProcess(label, getLabelStats());
        }
    }

    /**
     * Undo (revert) the normalization applied by this DataNormalization instance to the specified features array
     *
     * @param features Features to revert the normalization on
     */
    @Override
    public void revertFeatures(INDArray features) {
        strategy.revert(features, getFeatureStats());
    }

    /**
     * Undo (revert) the normalization applied by this DataNormalization instance to the specified labels array.
     * If labels normalization is disabled (i.e., {@link #isFitLabel()} == false) then this is a no-op.
     * Can also be used to undo normalization for network output arrays, in the case of regression.
     *
     * @param labels Labels array to revert the normalization on
     */
    @Override
    public void revertLabels(INDArray labels) {
        if (isFitLabel()) {
            strategy.revert(labels, getLabelStats());
        }
    }

    /**
     * Revert the data to what it was before transform
     *
     * @param data the dataset to revert back
     */
    @Override
    public void revert(DataSet data) {
        revertFeatures(data.getFeatures());
        revertLabels(data.getLabels());
    }
}

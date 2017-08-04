package org.social.mf;

import algorithms.MF_fastALS;
import data_structure.Rating;
import data_structure.SparseMatrix;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.itemrec.MF;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by GaoZhen on 2017/8/4.
 */
public class FastMf extends MF {

    double w0 = 10;
    boolean showProgress = false;
    boolean showLoss = true;
    double reg = 0.01;
    double alpha = 0.75;
    int userCount;
    int itemCount;

    int topK = 100;
    int threadNum = 10;

    double init_mean = 0;
    double init_stdev = 0.01;

    protected MF_fastALS mMfCore;


    public FastMf() {
    }

    @Override
    public void setFeedback(IPosOnlyFeedback feedback) {
        super.setFeedback(feedback);
        mMfCore = new MF_fastALS(
                makeInputMatrix(),
                new ArrayList<Rating>(),
                topK, threadNum,
                this.numFactors, this.numIter, w0, alpha, reg, init_mean, init_stdev, showProgress, showLoss);
    }

    private SparseMatrix makeInputMatrix() {
        userCount = feedback.maxUserID();
        itemCount = feedback.maxItemID();
        SparseMatrix matrix = new SparseMatrix(userCount, itemCount);
        IBooleanMatrix iMatrix = feedback.userMatrix();
        for (int i = 0; i < userCount; i++) {
            for (int j = 0; j < itemCount; j++) {
                boolean item = iMatrix.get(i, j);
                if (item) {
                    matrix.setValue(i, j, 1);
                } else {
                    matrix.setValue(i, j, 0);
                }
            }
        }
        return matrix;
    }

    @Override
    public void saveModel(String filename) throws IOException {
        Matrix<Double> userFactors = getUserFactors();
        for (int i = 0; i < userCount; i++) {
            for (int j = 0; j < numFactors; j++) {
                userFactors.set(i, j, mMfCore.U.get(i, j));
            }
        }
        Matrix<Double> itemFactors = getItemFactors();
        for (int i = 0; i < itemCount; i++) {
            for (int j = 0; j < numFactors; j++) {
                itemFactors.set(i, j, mMfCore.V.get(i, j));
            }
        }
        super.saveModel(filename);
    }

    @Override
    public void iterate() {
        // Update user latent vectors
        for (int u = 0; u < mMfCore.userCount; u++) {
            mMfCore.update_user(u);
        }

        // Update item latent vectors
        for (int i = 0; i < mMfCore.itemCount; i++) {
            mMfCore.update_item(i);
        }
    }

    @Override
    public double computeLoss() {
        return 0;
    }
}

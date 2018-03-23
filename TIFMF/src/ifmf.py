# -*- coding: UTF-8 -*-
import numpy as np
import scipy.sparse as sparse
import time

def alternating_least_squares_cg(Cui, factors, regularization=0.01, iterations=15):
    users, items = Cui.shape
    # 初始化随机因子
    X = np.random.rand(users, factors) * 0.01
    Y = np.random.rand(items, factors) * 0.01
    Cui, Ciu = Cui.tocsr(), Cui.T.tocsr()
    for iteration in xrange(iterations):
        t0 = time.time()
        least_squares_cg(Cui, X, Y, regularization)
        least_squares_cg(Ciu, Y, X, regularization)
        t1 = time.time()
        print(str(iteration) + ' times iteration cost:' + str(t1-t0))
    return X, Y
def least_squares_cg(Cui, X, Y, regularization, cg_steps=3):
    users, factors = X.shape
    YtY = Y.T.dot(Y) + regularization * np.eye(factors)
    for u in range(users):
        # 从前次迭代开始
        x = X[u]
        # 计算 r = YtCuPu - (YtCuY.dot(Xu), 不用计算YtCuY
        r = -YtY.dot(x)
        for i, confidence in nonzeros(Cui, u):
            r += (confidence - (confidence - 1) * Y[i].dot(x)) * Y[i]
        p = r.copy()
        rsold = r.dot(r)
        for it in range(cg_steps):
            # 计算Ap = YtCuYp 而不是 YtCuY
            Ap = YtY.dot(p)
            for i, confidence in nonzeros(Cui, u):
                Ap += (confidence - 1) * Y[i].dot(p) * Y[i]
            # 标准CG更新
            alpha = rsold / p.dot(Ap)
            x += alpha * p
            r -= alpha * Ap
            rsnew = r.dot(r)
            p = r + (rsnew / rsold) * p
            rsold = rsnew
        X[u] = x

def nonzeros(m, row):
    """ returns the non zeroes of a row in csr_matrix """
    for index in range(m.indptr[row], m.indptr[row+1]):
        yield m.indices[index], m.data[index]
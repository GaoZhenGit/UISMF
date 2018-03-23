import scipy.sparse as sparse
import sys
sys.path.append('./mf')
import ifmf
from multiprocessing import Process

def doMatrixF(inputFile, p, q, factor):
    with open(inputFile) as file:
        for row, line in enumerate(file):
            if row == 0:
                num_users, num_items = line.strip().split('*')
                num_users = int(num_users)
                num_items = int(num_items)
                matrix = sparse.lil_matrix((num_users, num_items), dtype=float)
            else:
                glist = line.strip().split(' ')
                if len(glist) > 1:
                    for gi in glist:
                        gi = gi.split(':')
                        col = gi[0]
                        matrix[int(row - 1), int(col)] = float(gi[1])
    P, Q = ifmf.alternating_least_squares_cg(matrix, factor, regularization=0.01, iterations=15)

    print('start outputing P:')
    Pfile = open(p, 'w')
    xi, xj = P.shape
    for i in xrange(xi):
        for j in xrange(xj):
            print_value = P[i, j]
            if print_value >= 0:
                Pfile.write("%.15f" % print_value)
            else:
                Pfile.write("%.14f" % print_value)
            Pfile.write(' ')
        Pfile.write('\n')
    Pfile.close()

    print('start outputing Q:')
    Qfile = open(q, 'w')
    Q = Q.T
    yi, yj = Q.shape
    for i in xrange(yi):
        for j in xrange(yj):
            print_value = Q[i, j]
            if print_value >= 0:
                Qfile.write("%.15f" % print_value)
            else:
                Qfile.write("%.14f" % print_value)
            Qfile.write(' ')
        Qfile.write('\n')
    Qfile.close()

if __name__ == '__main__':
    inputMatrix = sys.argv[1]
    p = sys.argv[2]
    q = sys.argv[3]
    factor = int(sys.argv[4])
    doMatrixF(inputMatrix, p,q, factor)

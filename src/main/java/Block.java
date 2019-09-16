import java.util.List;

/**
 * Created by alexandr on 26.06.19.
 */
public class Block {
    int blockSize;
    int transactionCounter;
    int previousblockHash;
    int merkleRootHash;
    int time;
    int complexity;
    int nonce;

    List<Transaction> transactions;
}

/**
 * Created by alexandr on 26.06.19.
 */

import com.mongodb.*;

import java.io.File;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import org.bitcoinj.core.*;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.utils.BlockFileLoader;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.util.ArrayList;
import java.util.List;


public class BitcoinParser {
    int BLOCKS_SIZE_LOCKER = 1000;
    public static final String blockchainPath = "/home/alexandr/Загрузки/blockchain/blocks";

    private NetworkParameters np;
    private MongoClient mongoClient;
    private DB database;


    public BitcoinParser(){
        np = new MainNetParams();
        try {
            mongoClient = new MongoClient();
        } catch(UnknownHostException e){
            System.out.println("UnknownHostException");
            e.printStackTrace();
        }
        database = mongoClient.getDB("Bitcoin");
        Context.getOrCreate(MainNetParams.get());
    }

    void parseTransactions(){
        DBCollection transaction = database.getCollection("Transaction");

        List<File> blockChainFiles = new ArrayList<File>();

        File file = new File("/home/alexandr/Загрузки/blockchain/blocks/blk00000.dat");

        blockChainFiles.add(file);

        BlockFileLoader bfl = new BlockFileLoader(np, blockChainFiles);

        int n = 0;

        for(Block block: bfl) {
            if(n >= BLOCKS_SIZE_LOCKER){
                return;
            } else {
                n++;
            }
            List<Transaction> transactions = block.getTransactions();
            for(Transaction ts: transactions){
                BasicDBObject transactionDocument = new BasicDBObject();

                transactionDocument.put("id", ts.getHash().toString());

                BasicDBList outputList = new BasicDBList();
                BasicDBList inputList = new BasicDBList();

                for(TransactionOutput to: ts.getOutputs()){
                    Address addr;

                    switch(to.getScriptPubKey().getScriptType()){
                        case P2PKH:
                            addr = to.getAddressFromP2PKHScript(np);
                            break;
                        case P2SH:
                            addr = to.getAddressFromP2SH(np);
                            break;
                        case PUB_KEY:
                            addr = new Address(np, hash160(to.getScriptPubKey().getPubKey()));
                            break;
                        default:
                            System.out.println("Skipped");
                            continue;
                    }

                    outputList.add(new BasicDBObject("index", to.getIndex()).append("addr", addr.toString()));
                }

                for(TransactionInput ti: ts.getInputs()){
                    inputList.add(new BasicDBObject("description", ti.toString()));
                }

                transactionDocument.put("inputs", inputList);
                transactionDocument.put("outputs", outputList);

                transaction.insert(transactionDocument);
            }
        }
    }

    public void parseUsers(){
        DBCollection user = database.getCollection("User");
        DBCollection transaction = database.getCollection("Transaction");
        List<File> blockChainFiles = new ArrayList<File>();

        File file = new File("/home/alexandr/Загрузки/blockchain/blocks/blk00000.dat");

        blockChainFiles.add(file);

        BlockFileLoader bfl = new BlockFileLoader(np, blockChainFiles);

        int n = 0;

        for(Block block: bfl){
            if(n >= BLOCKS_SIZE_LOCKER){
                return;
            } else {
                n++;
            }
            List<Transaction> transactions = block.getTransactions();
            for(Transaction ts: transactions){


                for(TransactionOutput to: ts.getOutputs()){

                    try{
                        Address addr;

                        switch(to.getScriptPubKey().getScriptType()){
                            case P2PKH:
                                addr = to.getAddressFromP2PKHScript(np);
                                break;
                            case P2SH:
                                addr = to.getAddressFromP2SH(np);
                                break;
                            case PUB_KEY:
                                addr = new Address(np, hash160(to.getScriptPubKey().getPubKey()));
                                break;
                            default:
                                System.out.println("Skipped");
                                continue;
                        }


                        BasicDBObject where = new BasicDBObject();

                        where.put("id", addr.toString());

                        DBObject doc = user.findOne(where);

                        if(doc == null){
                            BasicDBObject nDocument = new BasicDBObject();
                            nDocument.put("id", addr.toString());

                            BasicDBList list = new BasicDBList();

                            list.add(new BasicDBObject("trasaction_id", ts.getHash().toString())
                                    .append("index", to.getIndex())
                                    .append("value", to.getValue().getValue()));

                            nDocument.put("outputs", list);

                            user.insert(nDocument);
                        } else {
                            DBObject findQuery = new BasicDBObject("id", addr.toString());
                            DBObject listItem = new BasicDBObject("outputs", new BasicDBObject("transaction_id", to.toString()).
                                    append("index", to.getIndex()).
                                    append("value", to.getValue().toString()));
                            DBObject updateQuery = new BasicDBObject("$push", listItem);

                            user.update(findQuery, updateQuery);
                        }
                    } catch (Exception e){ /// !!!!!
                        System.out.println(e.toString());
                        e.printStackTrace();
                    }

                }

                for(TransactionInput in: ts.getInputs()){

                    if(in.isCoinBase()){
                        continue;
                    }

                    Sha256Hash hash = in.getOutpoint().getHash();
                    long index = in.getOutpoint().getIndex();


                    BasicDBObject query = new BasicDBObject("id", hash.toString()).
                            append("outputs.index", index);

                    DBObject doc = transaction.findOne(query);


                    if(doc == null){
                        System.out.println(in.isCoinBase());
                        System.out.println(hash);
                        System.out.println("Alarm!!!!!! Cant find such transaction id:" + hash.toString() + "  index:" + index);
                        return;
                    } else {
                        BasicDBList outputs = (BasicDBList) doc.get("outputs");
                        BasicDBObject output = (BasicDBObject) outputs.get(0);
                        String addr = (String) output.get("addr");

                        BasicDBObject userQuery = new BasicDBObject("addr", addr);

                        BasicDBObject elemToDelete = new BasicDBObject("transaction_id", hash.toString()).
                                append("index", index);

                        BasicDBObject deleteQuery = new BasicDBObject("$pull", elemToDelete);
                        System.out.println("deleted");
                        user.update(userQuery, deleteQuery);
                    }
                }
            }
        }

    }

    static byte[] hash160(byte[] in) { /// https://bitcoin.stackexchange.com/questions/50370/how-to-retrieve-the-from-and-to-wallet-addresses-of-a-transaction
        MessageDigest d1;
        try {
            d1 = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        d1.update(in);
        byte[] digest = d1.digest();
        RIPEMD160Digest d2 = new RIPEMD160Digest();
        d2.update(digest, 0, 32);
        byte[] ret = new byte[20];
        d2.doFinal(ret, 0);
        return ret;
    }

    final static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void main(String args[]){
        BitcoinParser bp = new BitcoinParser();
        bp.parseTransactions();
        bp.parseUsers();
///        System.out.println(getBlocksFrom("/home/alexandr/Загрузки/blockchain/blocks/blk00004.dat"));
    }
}

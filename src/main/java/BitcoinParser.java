/**
 * Created by alexandr on 26.06.19.
 */

import com.mongodb.*;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bitcoinj.core.*;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.utils.BlockFileLoader;
import org.bson.Document;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.util.ArrayList;
import java.util.List;


public class BitcoinParser {
    int BLOCKS_SIZE_LOCKER = 1000;
    private String blockchainPath = "/home/alexandr/Загрузки/blockchain/blocks";

    private NetworkParameters np;
    private MongoClient mongoClient;
//    private DB database;
    private MongoDatabase database;

    public BitcoinParser(String path, MongoClient mongoClient, MongoDatabase database){
        this.blockchainPath = path;
        this.np = new MainNetParams();
        Context.getOrCreate(MainNetParams.get());
        this.mongoClient = mongoClient;
        this.database = database;
    }

    public void startParsing(){
        parseTransactions();
        parseUsers();
    }
/*
    public BitcoinParser() {
        np = new MainNetParams();

        mongoClient = new MongoClient();
        database = mongoClient.getDB("Bitcoin");
        Context.getOrCreate(MainNetParams.get());
    }
*/
    void parseTransactions(){
        MongoCollection<Document> transaction = database.getCollection("Transaction");

        List<File> blockChainFiles = new ArrayList<File>();

///        File file = new File("/home/alexandr/Загрузки/blockchain/blocks/blk00000.dat");

        File file = getFileFromPath();

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
                Document transactionDocument = new Document();

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

                transaction.insertOne(transactionDocument);
            }
        }
    }

    public void parseUsers(){
        MongoCollection<Document> user = database.getCollection("User");
        MongoCollection<Document> transaction = database.getCollection("Transaction");
        List<File> blockChainFiles = new ArrayList<File>();

///        File file = new File("/home/alexandr/Загрузки/blockchain/blocks/blk00000.dat");

        File file = getFileFromPath();

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

                        Document doc = user.find(where).first();

//                        DBObject doc = user.findOne(where);

                        if(doc == null){
                            Document nDocument = new Document();
                            nDocument.put("id", addr.toString());

                            BasicDBList list = new BasicDBList();

                            list.add(new BasicDBObject("trasaction_id", ts.getHash().toString())
                                    .append("index", to.getIndex())
                                    .append("value", to.getValue().getValue()));

                            nDocument.put("outputs", list);

                            user.insertOne(nDocument);
                        } else {
                            Document findQuery = new Document("id", addr.toString());
                            Document listItem = new Document("outputs", new BasicDBObject("transaction_id", to.toString()).
                                    append("index", to.getIndex()).
                                    append("value", to.getValue().toString()));
                            Document updateQuery = new Document("$push", listItem);

                            user.updateOne(findQuery, updateQuery);
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


                    Document query = new Document("id", hash.toString()).
                            append("outputs.index", index);

                    Document doc = transaction.find(query).first();


                    if(doc == null){
                        System.out.println(in.isCoinBase());
                        System.out.println(hash);
                        System.out.println("Alarm!!!!!! Cant find such transaction id:" + hash.toString() + "  index:" + index);
//                        return;
                        continue;
                    } else {
//                        BasicDBList outputs = (BasicDBList) doc.get("outputs");
                        List<Document> outputs = (List<Document>) doc.get("outputs");
                        Document output = (Document) outputs.get(0);
                        String addr = (String) output.get("addr");

                        Document userQuery = new Document("addr", addr);

                        Document elemToDelete = new Document("transaction_id", hash.toString()).
                                append("index", index);

                        Document deleteQuery = new Document("$pull", elemToDelete);
                        System.out.println("deleted");
                        user.updateOne(userQuery, deleteQuery);
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


    private File getFileFromPath(){
        Path path = Paths.get(blockchainPath);

        File result = null;

        try{
            DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.dat");
            result =  stream.iterator().next().toFile();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void main(String args[]){
///        BitcoinParser bp = new BitcoinParser();
///        bp.parseTransactions();
///        bp.parseUsers();
///        System.out.println(getBlocksFrom("/home/alexandr/Загрузки/blockchain/blocks/blk00004.dat"));
    }
}

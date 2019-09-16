import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;

/**
 * Created by alexandr on 15.09.19.
 */
public class DBRunner {
    MongoClient mongoClient;
    MongoDatabase database;
    MongoCollection<Document> collection;
    String path;

    public DBRunner(){
        mongoClient = new MongoClient();
    }

    public boolean tryConnectToDB(String name){
        if(!mongoClient.getDatabaseNames().contains(name)){
            return false;
        }
        database = mongoClient.getDatabase(name);
        return true;
    }

    public void connectToDB(String name){
        database = mongoClient.getDatabase(name);
    }

    public boolean tryGetCollection(String name){
        if(!checkExistance(database.listCollectionNames(), name)){
            return false;
        }

        collection = database.getCollection(name);
        return true;
    }

    public void createCollection(String name){
        database.createCollection(name, null);
        collection = database.getCollection(name);
    }


    private boolean checkExistance(MongoIterable<String> collection, String s){
        MongoCursor<String> iterator = collection.iterator();

        while(iterator.hasNext()){
            if(iterator.next().equals(s)){
                return true;
            }
        }
        return false;
    }

    public void setPath(String path){
        this.path = path;
    }

    public BitcoinParser initParsing(){
        return new BitcoinParser(path, mongoClient, database);
    }
}

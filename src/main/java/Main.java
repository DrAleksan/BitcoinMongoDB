import com.mongodb.*;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;

import java.net.UnknownHostException;
import java.util.Arrays;

import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by alexandr on 02.05.19.
 */
public class Main {
    public static void main(String[] args){

        DBRunner runner = new DBRunner();

        UserInterface ui = new UserInterface(runner);

        /*
        MongoClient mongoClient = new MongoClient();

        MongoDatabase database = mongoClient.getDatabase("Bitcoin");


        database.createCollection("Bitcoin", null);

        Document document = new Document();



        MongoCollection<Document> collection =  database.getCollection("customers");


        document.put("name", "Shubham");
        document.put("company", "Baeldung");

        collection.insertOne(document);

        System.out.println(mongoClient.getDatabaseNames());
        */
    }
}
